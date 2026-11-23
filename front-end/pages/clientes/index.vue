<script setup lang="ts">
import { CLIENTE_STATUS_OPTIONS } from '~/schemas/cliente.schema'
import type { ClienteRecord } from '~/stores/clientes'

definePageMeta({
  layout: 'default',
  middleware: 'auth'
})

useHead({
  title: 'Clientes & Contratos — DRONE-MONITOR'
})

const authStore = useAuthStore()
const clientesStore = useClientesStore()
const empresasStore = useEmpresasStore()
const filtersStore = useFiltersStore()
const { operations } = useDashboardData()
const { formatDateBR } = useDateHelper()
const toast = useToast()

const { search, debouncedSearch } = useDebounceSearch('', 250)
const selectedStatus = ref<string>('Todos')
const selectedEmpresa = ref<string>('Todas')

const page = ref(1)
const pageSize = ref(8)

const formModalOpen = ref(false)
const editingCliente = ref<ClienteRecord | null>(null)
const viewingCliente = ref<ClienteRecord | null>(null)
const deletingCliente = ref<ClienteRecord | null>(null)

const columns = [
  { key: 'nome', label: 'Cliente / Demanda', sortable: true },
  { key: 'cpfRg', label: 'CPF/CNPJ & RG', sortable: true },
  { key: 'contato', label: 'Telefones' },
  { key: 'empresa', label: 'Operadora Vinculada' },
  { key: 'voos', label: 'Voos' },
  { key: 'statusContrato', label: 'Contrato', sortable: true },
  { key: 'acoes', label: 'Ações' }
]

const statusOptions = ['Todos', ...CLIENTE_STATUS_OPTIONS]

const empresaOptions = computed(() => [
  { label: 'Todas as operadoras', value: 'Todas' },
  ...empresasStore.empresas.map((e) => ({
    label: e.razaoSocial,
    value: e.cnpj
  }))
])

function getOperatorName(cnpj: string): string {
  return empresasStore.empresas.find((e) => e.cnpj === cnpj)?.razaoSocial || cnpj
}

function getClientFlightsCount(nome: string): number {
  const lower = nome.toLowerCase()
  return operations.value.filter((op) => op.clientName.toLowerCase() === lower).length
}

const filteredClientes = computed(() => {
  const localQ = debouncedSearch.value.trim().toLowerCase()
  const globalQ = filtersStore.globalSearch.trim().toLowerCase()
  const query = localQ || globalQ

  return clientesStore.clientes.filter((cliente) => {
    if (selectedStatus.value !== 'Todos' && cliente.statusContrato !== selectedStatus.value) {
      return false
    }
    if (selectedEmpresa.value !== 'Todas' && cliente.empresaOpeCnpj !== selectedEmpresa.value) {
      return false
    }
    if (!query) return true

    const haystack = [
      cliente.nome,
      cliente.cpfRg,
      cliente.cpfRg.replace(/\D/g, ''),
      cliente.rg,
      cliente.demandaPrincipal || '',
      ...cliente.telefones,
      getOperatorName(cliente.empresaOpeCnpj)
    ]
      .join(' ')
      .toLowerCase()

    return haystack.includes(query)
  })
})

const paginatedClientes = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredClientes.value.slice(start, start + pageSize.value)
})

watch([debouncedSearch, selectedStatus, selectedEmpresa], () => {
  page.value = 1
})

function openCreateModal() {
  editingCliente.value = null
  formModalOpen.value = true
}

function openEditModal(cliente: ClienteRecord) {
  viewingCliente.value = null
  editingCliente.value = cliente
  formModalOpen.value = true
}

async function handleToggleStatus(cliente: ClienteRecord) {
  try {
    const updated = await clientesStore.toggleContratoStatus(cliente.cpfRg)
    toast.add({
      title: `Contrato ${updated.statusContrato}`,
      description: `Contrato de ${updated.nome} alterado para ${updated.statusContrato.toLowerCase()}.`,
      icon: updated.statusContrato === 'Ativo' ? 'i-lucide-check-circle-2' : 'i-lucide-pause-circle',
      color: updated.statusContrato === 'Ativo' ? 'green' : 'amber'
    })
  } catch (err) {
    toast.add({
      title: 'Erro ao atualizar contrato',
      description: extractApiErrorMessage(err),
      icon: 'i-lucide-alert-triangle',
      color: 'red'
    })
  }
}

async function confirmDeleteCliente() {
  if (!deletingCliente.value) return
  const nome = deletingCliente.value.nome
  const doc = deletingCliente.value.cpfRg
  try {
    await clientesStore.removeCliente(doc)
    deletingCliente.value = null
    toast.add({
      title: 'Cliente removido',
      description: `${nome} foi removido do diretório de clientes.`,
      icon: 'i-lucide-trash-2',
      color: 'blue'
    })
  } catch (err) {
    toast.add({
      title: 'Erro ao remover cliente',
      description: extractApiErrorMessage(err),
      icon: 'i-lucide-alert-triangle',
      color: 'red'
    })
  }
}

onMounted(() => {
  clientesStore.fetchClientes()
  empresasStore.fetchEmpresas()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold text-slate-900 sm:text-2xl">
          Clientes & Contratos
        </h1>
        <p class="mt-1 text-sm text-slate-500">
          Diretório de clientes tomadores de serviço, contratos formalizados com operadoras e histórico de voos.
        </p>
      </div>

      <div class="flex items-center gap-2.5">
        <UButton
          color="gray"
          variant="outline"
          icon="i-lucide-refresh-cw"
          :loading="clientesStore.loading"
          @click="clientesStore.fetchClientes(true)"
        >
          Atualizar
        </UButton>
        <UButton color="blue" icon="i-lucide-plus" @click="openCreateModal">
          Novo Cliente
        </UButton>
      </div>
    </div>

    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <CommonStatCard
        label="Carteira de Clientes"
        :value="clientesStore.stats.total"
        meta="Tomadores cadastrados"
        tone="blue"
      />
      <CommonStatCard
        label="Contratos Ativos"
        :value="clientesStore.stats.ativos"
        meta="Aptos para solicitações"
        tone="green"
      />
      <CommonStatCard
        label="Contratos Inativos"
        :value="clientesStore.stats.inativos"
        meta="Encerrados ou suspensos"
        tone="amber"
      />
      <CommonStatCard
        label="Operadoras Atendendo"
        :value="clientesStore.stats.operadorasVinculadas"
        meta="Empresas com contratos"
        tone="neutral"
      />
    </div>

    <section class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div class="flex flex-wrap items-end justify-between gap-4 border-b border-slate-200 px-5 py-4">
        <div>
          <h2 class="text-base font-semibold text-slate-900">
            Diretório de Clientes
          </h2>
          <p class="text-xs text-slate-500">
            Clique em <strong>Ficha</strong> para visualizar o contrato e o histórico de voos do tomador.
          </p>
        </div>

        <div class="flex flex-wrap items-end gap-2.5">
          <div class="flex flex-col gap-1">
            <label for="cli-search" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              CPF/RG ou Nome
            </label>
            <UInput
              id="cli-search"
              v-model="search"
              size="sm"
              icon="i-lucide-search"
              placeholder="Buscar nome, CPF, RG..."
              class="w-52"
            />
          </div>

          <div class="flex flex-col gap-1">
            <label for="cli-status" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Contrato
            </label>
            <USelect
              id="cli-status"
              v-model="selectedStatus"
              :options="statusOptions"
              size="sm"
            />
          </div>

          <div v-if="authStore.isAdmin" class="flex flex-col gap-1">
            <label for="cli-empresa" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Operadora
            </label>
            <USelect
              id="cli-empresa"
              v-model="selectedEmpresa"
              :options="empresaOptions"
              option-attribute="label"
              value-attribute="value"
              size="sm"
            />
          </div>
        </div>
      </div>

      <UTable
        :rows="paginatedClientes"
        :columns="columns"
        :loading="clientesStore.loading"
        :empty-state="{ icon: 'i-lucide-building-2', label: 'Nenhum cliente encontrado para os filtros aplicados.' }"
        :ui="{
          th: {
            base: 'code-mono uppercase tracking-wider text-[11px] bg-slate-50 text-slate-500'
          },
          td: {
            base: 'text-sm text-slate-800'
          }
        }"
      >
        <template #nome-data="{ row }">
          <div>
            <span class="font-semibold text-slate-900">{{ row.nome }}</span>
            <span v-if="row.demandaPrincipal" class="block max-w-[230px] truncate text-xs text-slate-500">
              {{ row.demandaPrincipal }}
            </span>
          </div>
        </template>

        <template #cpfRg-data="{ row }">
          <div>
            <span class="code-mono block text-xs font-semibold text-slate-800">{{ row.cpfRg }}</span>
            <span class="code-mono block text-[11px] text-slate-400">RG {{ row.rg }}</span>
          </div>
        </template>

        <template #contato-data="{ row }">
          <span class="code-mono text-xs text-slate-600">
            {{ row.telefones[0] || '—' }}
            <template v-if="row.telefones.length > 1">
              (+{{ row.telefones.length - 1 }})
            </template>
          </span>
        </template>

        <template #empresa-data="{ row }">
          <div>
            <span class="block max-w-[175px] truncate text-xs font-medium text-slate-800" :title="getOperatorName(row.empresaOpeCnpj)">
              {{ getOperatorName(row.empresaOpeCnpj) }}
            </span>
            <span class="code-mono block text-[11px] text-slate-400">
              Desde {{ formatDateBR(row.dataFormalizacao) }}
            </span>
          </div>
        </template>

        <template #voos-data="{ row }">
          <span class="code-mono rounded bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-700">
            {{ getClientFlightsCount(row.nome) }}
          </span>
        </template>

        <template #statusContrato-data="{ row }">
          <div class="flex items-center gap-2">
            <UToggle
              :model-value="row.statusContrato === 'Ativo'"
              color="green"
              size="sm"
              :aria-label="`Alternar contrato de ${row.nome}`"
              @update:model-value="handleToggleStatus(row)"
            />
            <UBadge
              :color="row.statusContrato === 'Ativo' ? 'green' : 'gray'"
              variant="subtle"
              size="xs"
              class="code-mono"
            >
              {{ row.statusContrato }}
            </UBadge>
          </div>
        </template>

        <template #acoes-data="{ row }">
          <div class="flex items-center gap-1">
            <UButton
              color="gray"
              variant="ghost"
              size="xs"
              icon="i-lucide-folder-clock"
              @click="viewingCliente = row"
            >
              Ficha
            </UButton>
            <UButton
              color="gray"
              variant="ghost"
              size="xs"
              icon="i-lucide-pencil"
              aria-label="Editar cliente"
              @click="openEditModal(row)"
            />
            <UButton
              color="red"
              variant="ghost"
              size="xs"
              icon="i-lucide-trash-2"
              aria-label="Excluir cliente"
              @click="deletingCliente = row"
            />
          </div>
        </template>
      </UTable>

      <div class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 bg-slate-50 px-5 py-3 text-xs text-slate-600">
        <span>
          Exibindo <strong>{{ paginatedClientes.length }}</strong> de <strong>{{ filteredClientes.length }}</strong> clientes
        </span>
        <UPagination
          v-model="page"
          :page-count="pageSize"
          :total="filteredClientes.length"
          size="sm"
        />
      </div>
    </section>

    <FormsClienteFormModal
      v-model="formModalOpen"
      :editing-cliente="editingCliente"
    />

    <CommonClienteFichaModal
      :cliente="viewingCliente"
      @close="viewingCliente = null"
      @edit="openEditModal"
    />

    <UModal :model-value="Boolean(deletingCliente)" @update:model-value="!$event && (deletingCliente = null)">
      <UCard v-if="deletingCliente">
        <template #header>
          <div class="flex items-center gap-2 text-red-700">
            <UIcon name="i-lucide-trash-2" class="h-5 w-5" />
            <h3 class="text-base font-semibold">Remover cliente</h3>
          </div>
        </template>
        <p class="text-sm text-slate-600">
          Confirma a remoção de <strong class="text-slate-900">{{ deletingCliente.nome }}</strong>
          (<span class="code-mono">{{ deletingCliente.cpfRg }}</span>)?
        </p>
        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="gray" variant="outline" @click="deletingCliente = null">Cancelar</UButton>
            <UButton color="red" :loading="clientesStore.loading" @click="confirmDeleteCliente">Remover</UButton>
          </div>
        </template>
      </UCard>
    </UModal>
  </div>
</template>
