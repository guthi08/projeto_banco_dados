<script setup lang="ts">
import { SETORES_ATUACAO, STATUS_CONTRATO_OPTIONS } from '~/schemas/empresa.schema'
import type { EmpresaOperadoraRecord } from '~/stores/empresas'

definePageMeta({
  layout: 'default',
  middleware: ['auth', 'admin']
})

useHead({
  title: 'Empresas Operadoras — DRONE-MONITOR'
})

const empresasStore = useEmpresasStore()
const filtersStore = useFiltersStore()
const toast = useToast()

const { search, debouncedSearch } = useDebounceSearch('', 250)
const selectedSetor = ref<string>('Todos')
const selectedStatus = ref<string>('Todos')

const page = ref(1)
const pageSize = ref(6)

const formModalOpen = ref(false)
const editingEmpresa = ref<EmpresaOperadoraRecord | null>(null)
const viewingEmpresa = ref<EmpresaOperadoraRecord | null>(null)

const columns = [
  { key: 'razaoSocial', label: 'Razão Social / CNPJ', sortable: true },
  { key: 'setorAtuacao', label: 'Setor', sortable: true },
  { key: 'contato', label: 'Contato' },
  { key: 'recursos', label: 'Frota / Pilotos' },
  { key: 'statusContrato', label: 'Status Contrato', sortable: true },
  { key: 'acoes', label: 'Ações' }
]

const setorOptions = ['Todos', ...SETORES_ATUACAO]
const statusOptions = ['Todos', ...STATUS_CONTRATO_OPTIONS]
const pageSizeOptions = [
  { label: '6 por página', value: 6 },
  { label: '10 por página', value: 10 },
  { label: '20 por página', value: 20 }
]

const filteredEmpresas = computed(() => {
  const localQ = debouncedSearch.value.trim().toLowerCase()
  const globalQ = filtersStore.globalSearch.trim().toLowerCase()
  const query = localQ || globalQ

  return empresasStore.empresas.filter((empresa) => {
    if (selectedSetor.value !== 'Todos' && empresa.setorAtuacao !== selectedSetor.value) {
      return false
    }
    if (selectedStatus.value !== 'Todos' && empresa.statusContrato !== selectedStatus.value) {
      return false
    }
    if (!query) return true

    const haystack = [
      empresa.cnpj,
      empresa.cnpj.replace(/\D/g, ''),
      empresa.razaoSocial,
      empresa.email,
      empresa.setorAtuacao,
      ...empresa.telefones
    ]
      .join(' ')
      .toLowerCase()

    return haystack.includes(query)
  })
})

const paginatedEmpresas = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredEmpresas.value.slice(start, start + pageSize.value)
})

watch([debouncedSearch, selectedSetor, selectedStatus, pageSize], () => {
  page.value = 1
})

function openCreateModal() {
  editingEmpresa.value = null
  formModalOpen.value = true
}

function openEditModal(empresa: EmpresaOperadoraRecord) {
  viewingEmpresa.value = null
  editingEmpresa.value = empresa
  formModalOpen.value = true
}

async function handleToggleStatus(empresa: EmpresaOperadoraRecord) {
  try {
    const updated = await empresasStore.toggleStatus(empresa.cnpj)
    toast.add({
      title: `Contrato ${updated.statusContrato}`,
      description: `${updated.razaoSocial} agora está ${updated.statusContrato.toLowerCase()}.`,
      icon: updated.statusContrato === 'Ativo' ? 'i-lucide-check-circle-2' : 'i-lucide-pause-circle',
      color: updated.statusContrato === 'Ativo' ? 'green' : 'amber'
    })
  } catch (err) {
    toast.add({
      title: 'Erro ao atualizar status',
      description: extractApiErrorMessage(err),
      icon: 'i-lucide-alert-triangle',
      color: 'red'
    })
  }
}

function handleActivateOperator(cnpj: string) {
  empresasStore.setActiveOperator(cnpj)
  const op = empresasStore.activeOperator
  viewingEmpresa.value = null
  toast.add({
    title: 'Operadora ativa alterada',
    description: `Contexto do painel definido para ${op?.razaoSocial || cnpj}.`,
    icon: 'i-lucide-briefcase',
    color: 'blue'
  })
}

function statusBadgeColor(status: string): 'green' | 'amber' | 'gray' {
  if (status === 'Ativo') return 'green'
  if (status === 'Suspenso') return 'amber'
  return 'gray'
}

function isActiveOperator(cnpj: string): boolean {
  return empresasStore.activeOperator?.cnpj === cnpj
}

onMounted(() => {
  empresasStore.fetchEmpresas()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Cabeçalho da Página -->
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold text-slate-900 sm:text-2xl">
          Empresas Operadoras
        </h1>
        <p class="mt-1 text-sm text-slate-500">
          Gestão cadastral das operadoras credenciadas, setores de atuação e status contratual.
        </p>
      </div>

      <div class="flex items-center gap-2.5">
        <UButton
          color="gray"
          variant="outline"
          icon="i-lucide-refresh-cw"
          :loading="empresasStore.loading"
          @click="empresasStore.fetchEmpresas(true)"
        >
          Atualizar
        </UButton>
        <UButton color="blue" icon="i-lucide-plus" @click="openCreateModal">
          Nova Empresa
        </UButton>
      </div>
    </div>

    <!-- KPIs de Operadoras -->
    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <CommonStatCard
        label="Total de Operadoras"
        :value="empresasStore.stats.total"
        meta="Cadastradas na plataforma"
        tone="blue"
      />
      <CommonStatCard
        label="Contratos Ativos"
        :value="empresasStore.stats.ativas"
        meta="Aptas para operações"
        tone="green"
      />
      <CommonStatCard
        label="Inativos / Suspensos"
        :value="empresasStore.stats.inativasOuSuspensas"
        meta="Sem despacho ativo"
        tone="amber"
      />
      <CommonStatCard
        label="Setores Atendidos"
        :value="empresasStore.stats.setoresUnicos"
        meta="Segmentos aeronáuticos"
        tone="neutral"
      />
    </div>

    <!-- Card Principal com Filtros + UTable Paginada -->
    <section class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div class="flex flex-wrap items-end justify-between gap-4 border-b border-slate-200 px-5 py-4">
        <div>
          <h2 class="text-base font-semibold text-slate-900">
            Operadoras Credenciadas
          </h2>
          <p class="text-xs text-slate-500">
            Operadora em foco: <strong class="text-blue-700">{{ empresasStore.activeOperator?.razaoSocial }}</strong>
          </p>
        </div>

        <div class="flex flex-wrap items-end gap-2.5">
          <div class="flex flex-col gap-1">
            <label for="emp-search" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              CNPJ / Razão Social
            </label>
            <UInput
              id="emp-search"
              v-model="search"
              size="sm"
              icon="i-lucide-search"
              placeholder="Buscar CNPJ, razão social..."
              class="w-56"
            />
          </div>

          <div class="flex flex-col gap-1">
            <label for="emp-setor" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Setor
            </label>
            <USelect
              id="emp-setor"
              v-model="selectedSetor"
              :options="setorOptions"
              size="sm"
            />
          </div>

          <div class="flex flex-col gap-1">
            <label for="emp-status" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Status
            </label>
            <USelect
              id="emp-status"
              v-model="selectedStatus"
              :options="statusOptions"
              size="sm"
            />
          </div>
        </div>
      </div>

      <UTable
        :rows="paginatedEmpresas"
        :columns="columns"
        :loading="empresasStore.loading"
        :empty-state="{ icon: 'i-lucide-briefcase', label: 'Nenhuma empresa operadora encontrada para os filtros aplicados.' }"
        :ui="{
          th: {
            base: 'code-mono uppercase tracking-wider text-[11px] bg-slate-50 text-slate-500'
          },
          td: {
            base: 'text-sm text-slate-800'
          }
        }"
      >
        <template #razaoSocial-data="{ row }">
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <span class="font-semibold text-slate-900">{{ row.razaoSocial }}</span>
              <UBadge
                v-if="isActiveOperator(row.cnpj)"
                color="blue"
                variant="subtle"
                size="xs"
                class="code-mono"
              >
                ATIVA
              </UBadge>
            </div>
            <span class="code-mono block text-xs text-slate-500">{{ row.cnpj }}</span>
          </div>
        </template>

        <template #setorAtuacao-data="{ row }">
          <UBadge color="gray" variant="subtle" size="xs" class="font-medium">
            {{ row.setorAtuacao }}
          </UBadge>
        </template>

        <template #contato-data="{ row }">
          <div>
            <span class="block text-xs font-medium text-slate-800">{{ row.email }}</span>
            <span class="code-mono block text-xs text-slate-500">
              {{ row.telefones[0] || '—' }}
              <template v-if="row.telefones.length > 1">
                (+{{ row.telefones.length - 1 }})
              </template>
            </span>
          </div>
        </template>

        <template #recursos-data="{ row }">
          <div class="code-mono flex items-center gap-2 text-xs text-slate-600">
            <span class="rounded bg-slate-100 px-2 py-0.5">{{ row.dronesCount }} drones</span>
            <span class="rounded bg-slate-100 px-2 py-0.5">{{ row.pilotosCount }} pilotos</span>
          </div>
        </template>

        <template #statusContrato-data="{ row }">
          <div class="flex items-center gap-2.5">
            <UToggle
              :model-value="row.statusContrato === 'Ativo'"
              color="green"
              size="sm"
              :aria-label="`Alternar status de ${row.razaoSocial}`"
              @update:model-value="handleToggleStatus(row)"
            />
            <UBadge :color="statusBadgeColor(row.statusContrato)" variant="subtle" size="xs" class="code-mono">
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
              icon="i-lucide-file-check-2"
              title="Visualizar contrato"
              @click="viewingEmpresa = row"
            >
              Contrato
            </UButton>
            <UButton
              color="gray"
              variant="ghost"
              size="xs"
              icon="i-lucide-pencil"
              aria-label="Editar empresa"
              @click="openEditModal(row)"
            />
          </div>
        </template>
      </UTable>

      <!-- Rodapé Paginado -->
      <div class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 bg-slate-50 px-5 py-3">
        <div class="flex items-center gap-3 text-xs text-slate-600">
          <span>
            Exibindo <strong>{{ paginatedEmpresas.length }}</strong> de <strong>{{ filteredEmpresas.length }}</strong> operadoras
          </span>
          <USelect
            v-model="pageSize"
            :options="pageSizeOptions"
            option-attribute="label"
            value-attribute="value"
            size="xs"
          />
        </div>

        <UPagination
          v-model="page"
          :page-count="pageSize"
          :total="filteredEmpresas.length"
          size="sm"
        />
      </div>
    </section>

    <!-- Modais -->
    <FormsEmpresaFormModal
      v-model="formModalOpen"
      :editing-empresa="editingEmpresa"
    />

    <CommonEmpresaContratoModal
      :empresa="viewingEmpresa"
      @close="viewingEmpresa = null"
      @edit="openEditModal"
      @activate="handleActivateOperator"
    />
  </div>
</template>
