<script setup lang="ts">
import type { PilotoRecord } from '~/stores/pilotos'

definePageMeta({
  layout: 'default',
  middleware: 'auth'
})

useHead({
  title: 'Pilotos & Controle de CMA — DRONE-MONITOR'
})

const authStore = useAuthStore()
const pilotosStore = usePilotosStore()
const empresasStore = useEmpresasStore()
const filtersStore = useFiltersStore()
const toast = useToast()
const { formatDateBR, getValidityStatus } = useDateHelper()

const { search, debouncedSearch } = useDebounceSearch('', 250)
const selectedCma = ref<'Todos' | 'valid' | 'expiring' | 'expired'>('Todos')
const selectedEmpresa = ref<string>('Todas')

const page = ref(1)
const pageSize = ref(8)

const formModalOpen = ref(false)
const editingPiloto = ref<PilotoRecord | null>(null)
const viewingPiloto = ref<PilotoRecord | null>(null)
const deletingPiloto = ref<PilotoRecord | null>(null)

const columns = [
  { key: 'nome', label: 'Piloto / Contato', sortable: true },
  { key: 'cpf', label: 'CPF / RG', sortable: true },
  { key: 'numRegistroLicenca', label: 'Registro / Licença', sortable: true },
  { key: 'empresa', label: 'Empresa Operadora' },
  { key: 'validadeCma', label: 'Situação CMA', sortable: true },
  { key: 'acoes', label: 'Ações' }
]

const cmaOptions = [
  { label: 'Todas as situações', value: 'Todos' },
  { label: '🟢 CMA Válido (> 30 dias)', value: 'valid' },
  { label: '🟡 Vencendo (< 30 dias)', value: 'expiring' },
  { label: '🔴 CMA Vencido (Bloqueados)', value: 'expired' }
]

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

function isExpired(validadeCma: string): boolean {
  return getValidityStatus(validadeCma, 30) === 'expired'
}

const filteredPilotos = computed(() => {
  const localQ = debouncedSearch.value.trim().toLowerCase()
  const globalQ = filtersStore.globalSearch.trim().toLowerCase()
  const query = localQ || globalQ

  return pilotosStore.pilotos.filter((piloto) => {
    const level = getValidityStatus(piloto.validadeCma, 30)
    if (selectedCma.value !== 'Todos' && level !== selectedCma.value) return false
    if (selectedEmpresa.value !== 'Todas' && piloto.empresaOpeCnpj !== selectedEmpresa.value) return false

    if (!query) return true

    const haystack = [
      piloto.nome,
      piloto.cpf,
      piloto.cpf.replace(/\D/g, ''),
      piloto.rg,
      piloto.email,
      piloto.numRegistroLicenca,
      ...piloto.telefones,
      getOperatorName(piloto.empresaOpeCnpj)
    ]
      .join(' ')
      .toLowerCase()

    return haystack.includes(query)
  })
})

const paginatedPilotos = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredPilotos.value.slice(start, start + pageSize.value)
})

watch([debouncedSearch, selectedCma, selectedEmpresa], () => {
  page.value = 1
})

function openCreateModal() {
  editingPiloto.value = null
  formModalOpen.value = true
}

function openEditModal(piloto: PilotoRecord) {
  viewingPiloto.value = null
  editingPiloto.value = piloto
  formModalOpen.value = true
}

async function handleRenewCma(cpf: string) {
  try {
    const updated = await pilotosStore.renewCma(cpf, 12)
    viewingPiloto.value = updated
    toast.add({
      title: 'CMA renovado com sucesso',
      description: `Nova validade para ${updated.nome}: ${formatDateBR(updated.validadeCma)}.`,
      icon: 'i-lucide-shield-check',
      color: 'green'
    })
  } catch (err) {
    toast.add({
      title: 'Erro ao renovar CMA',
      description: extractApiErrorMessage(err),
      icon: 'i-lucide-alert-triangle',
      color: 'red'
    })
  }
}

async function confirmDeletePiloto() {
  if (!deletingPiloto.value) return
  const nome = deletingPiloto.value.nome
  const cpf = deletingPiloto.value.cpf
  try {
    await pilotosStore.removePiloto(cpf)
    deletingPiloto.value = null
    toast.add({
      title: 'Piloto removido',
      description: `${nome} foi removido do quadro de pilotos.`,
      icon: 'i-lucide-trash-2',
      color: 'blue'
    })
  } catch (err) {
    toast.add({
      title: 'Erro ao remover piloto',
      description: extractApiErrorMessage(err),
      icon: 'i-lucide-alert-triangle',
      color: 'red'
    })
  }
}

onMounted(() => {
  pilotosStore.fetchPilotos()
  empresasStore.fetchEmpresas()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Cabeçalho -->
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold text-slate-900 sm:text-2xl">
          Pilotos & Controle de CMA
        </h1>
        <p class="mt-1 text-sm text-slate-500">
          Gestão de pilotos habilitados, licenças ANAC, múltiplos telefones e validade do Certificado Médico Aeronáutico (CMA).
        </p>
      </div>

      <div class="flex items-center gap-2.5">
        <UButton
          color="gray"
          variant="outline"
          icon="i-lucide-refresh-cw"
          :loading="pilotosStore.loading"
          @click="pilotosStore.fetchPilotos(true)"
        >
          Atualizar
        </UButton>
        <UButton color="blue" icon="i-lucide-plus" @click="openCreateModal">
          Novo Piloto
        </UButton>
      </div>
    </div>

    <!-- KPIs de Pilotos -->
    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <CommonStatCard
        label="Quadro de Pilotos"
        :value="pilotosStore.stats.total"
        :meta="`${pilotosStore.stats.aptosPct}% aptos para escala`"
        tone="blue"
      />
      <CommonStatCard
        label="CMA Regular"
        :value="pilotosStore.stats.validos"
        meta="Validade acima de 30 dias"
        tone="green"
      />
      <CommonStatCard
        label="CMA Vencendo (< 30d)"
        :value="pilotosStore.stats.vencendo"
        meta="Renovação médica próxima"
        tone="amber"
        icon="i-lucide-clock-alert"
      />
      <CommonStatCard
        label="CMA Expirado"
        :value="pilotosStore.stats.vencidos"
        meta="Bloqueados para novas escalas"
        tone="red"
        icon="i-lucide-shield-x"
      />
    </div>

    <!-- Barra de Regra CMA -->
    <div class="flex flex-wrap items-center gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 text-xs text-slate-600 shadow-sm">
      <span class="code-mono font-semibold uppercase tracking-wider text-slate-500">Trava CMA:</span>
      <span class="inline-flex items-center gap-1.5">
        <span class="h-2 w-2 rounded-full bg-emerald-600" />
        <strong>Apto</strong> (liberado para escala)
      </span>
      <span class="inline-flex items-center gap-1.5">
        <span class="h-2 w-2 rounded-full bg-amber-500" />
        <strong>Vence em &lt; 30 dias</strong> (alerta preventivo)
      </span>
      <span class="inline-flex items-center gap-1.5">
        <span class="h-2 w-2 rounded-full bg-red-600" />
        <strong>Expirado</strong> (desabilitado visualmente e bloqueado no agendamento de missões)
      </span>
    </div>

    <!-- Card Principal com UTable -->
    <section class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div class="flex flex-wrap items-end justify-between gap-4 border-b border-slate-200 px-5 py-4">
        <div>
          <h2 class="text-base font-semibold text-slate-900">
            Pilotos Habilitados
          </h2>
          <p class="text-xs text-slate-500">
            Consulte a ficha para visualizar telefones adicionais ou renovar o CMA.
          </p>
        </div>

        <div class="flex flex-wrap items-end gap-2.5">
          <div class="flex flex-col gap-1">
            <label for="piloto-search" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Buscar Piloto
            </label>
            <UInput
              id="piloto-search"
              v-model="search"
              size="sm"
              icon="i-lucide-search"
              placeholder="Nome, CPF, licença..."
              class="w-52"
            />
          </div>

          <div class="flex flex-col gap-1">
            <label for="piloto-cma" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Situação CMA
            </label>
            <USelect
              id="piloto-cma"
              v-model="selectedCma"
              :options="cmaOptions"
              option-attribute="label"
              value-attribute="value"
              size="sm"
            />
          </div>

          <div v-if="authStore.isAdmin" class="flex flex-col gap-1">
            <label for="piloto-empresa" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Operadora
            </label>
            <USelect
              id="piloto-empresa"
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
        :rows="paginatedPilotos"
        :columns="columns"
        :loading="pilotosStore.loading"
        :empty-state="{ icon: 'i-lucide-user-check', label: 'Nenhum piloto encontrado para os filtros aplicados.' }"
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
          <div :class="isExpired(row.validadeCma) ? 'opacity-75' : ''">
            <div class="flex flex-wrap items-center gap-2">
              <span class="font-semibold text-slate-900">{{ row.nome }}</span>
              <UBadge
                v-if="isExpired(row.validadeCma)"
                color="red"
                variant="solid"
                size="xs"
                class="code-mono"
              >
                BLOQUEADO
              </UBadge>
            </div>
            <span class="block text-xs text-slate-500">{{ row.email }}</span>
            <span class="code-mono block text-[11px] text-slate-400">
              {{ row.telefones[0] || '—' }}
              <template v-if="row.telefones.length > 1">
                (+{{ row.telefones.length - 1 }})
              </template>
            </span>
          </div>
        </template>

        <template #cpf-data="{ row }">
          <div>
            <span class="code-mono block text-xs font-semibold text-slate-800">{{ row.cpf }}</span>
            <span class="code-mono block text-[11px] text-slate-400">RG {{ row.rg }}</span>
          </div>
        </template>

        <template #numRegistroLicenca-data="{ row }">
          <span class="code-mono rounded bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-800">
            {{ row.numRegistroLicenca }}
          </span>
        </template>

        <template #empresa-data="{ row }">
          <span class="block max-w-[180px] truncate text-xs font-medium text-slate-700" :title="getOperatorName(row.empresaOpeCnpj)">
            {{ getOperatorName(row.empresaOpeCnpj) }}
          </span>
        </template>

        <template #validadeCma-data="{ row }">
          <div class="space-y-1">
            <CommonValidityBadge :valid-until="row.validadeCma" domain-label="CMA" />
            <span class="code-mono block text-[11px] text-slate-500">
              {{ formatDateBR(row.validadeCma) }}
            </span>
          </div>
        </template>

        <template #acoes-data="{ row }">
          <div class="flex items-center gap-1">
            <UButton
              color="gray"
              variant="ghost"
              size="xs"
              icon="i-lucide-file-badge"
              @click="viewingPiloto = row"
            >
              Ficha / CMA
            </UButton>
            <UButton
              color="gray"
              variant="ghost"
              size="xs"
              icon="i-lucide-pencil"
              aria-label="Editar piloto"
              @click="openEditModal(row)"
            />
            <UButton
              color="red"
              variant="ghost"
              size="xs"
              icon="i-lucide-trash-2"
              aria-label="Excluir piloto"
              @click="deletingPiloto = row"
            />
          </div>
        </template>
      </UTable>

      <div class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 bg-slate-50 px-5 py-3 text-xs text-slate-600">
        <span>
          Exibindo <strong>{{ paginatedPilotos.length }}</strong> de <strong>{{ filteredPilotos.length }}</strong> pilotos
        </span>
        <UPagination
          v-model="page"
          :page-count="pageSize"
          :total="filteredPilotos.length"
          size="sm"
        />
      </div>
    </section>

    <!-- Modais -->
    <FormsPilotoFormModal
      v-model="formModalOpen"
      :editing-piloto="editingPiloto"
    />

    <CommonPilotoFichaModal
      :piloto="viewingPiloto"
      @close="viewingPiloto = null"
      @edit="openEditModal"
      @renew-cma="handleRenewCma"
    />

    <UModal :model-value="Boolean(deletingPiloto)" @update:model-value="!$event && (deletingPiloto = null)">
      <UCard v-if="deletingPiloto">
        <template #header>
          <div class="flex items-center gap-2 text-red-700">
            <UIcon name="i-lucide-trash-2" class="h-5 w-5" />
            <h3 class="text-base font-semibold">Remover piloto</h3>
          </div>
        </template>
        <p class="text-sm text-slate-600">
          Confirma a remoção do piloto <strong class="text-slate-900">{{ deletingPiloto.nome }}</strong>
          (<span class="code-mono">{{ deletingPiloto.cpf }}</span>)?
        </p>
        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="gray" variant="outline" @click="deletingPiloto = null">Cancelar</UButton>
            <UButton color="red" :loading="pilotosStore.loading" @click="confirmDeletePiloto">Remover</UButton>
          </div>
        </template>
      </UCard>
    </UModal>
  </div>
</template>
