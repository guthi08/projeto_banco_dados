<script setup lang="ts">
import type { StatusVoo } from '~/types/models'
import type { OperacaoVooRecord } from '~/stores/operacoes'
import type { StatusFilterValue } from '~/stores/filters'

definePageMeta({
  layout: 'default',
  middleware: 'auth'
})

useHead({
  title: 'Operações de Voo — Agenda & Despacho — DRONE-MONITOR'
})

const authStore = useAuthStore()
const operacoesStore = useOperacoesStore()
const empresasStore = useEmpresasStore()
const filtersStore = useFiltersStore()
const toast = useToast()
const { formatDateTimeBR } = useDateHelper()

const { search, debouncedSearch } = useDebounceSearch('', 250)
const viewMode = ref<'table' | 'calendar'>('table')
const selectedEmpresa = ref<string>('Todas')

const page = ref(1)
const pageSize = ref(8)

const statusOptions: { label: string; value: StatusFilterValue }[] = [
  { label: 'Todos os status', value: 'TODOS' },
  { label: 'Agendado', value: 'AGENDADO' },
  { label: 'Em Andamento', value: 'EM_ANDAMENTO' },
  { label: 'Concluído', value: 'CONCLUIDO' },
  { label: 'Cancelado', value: 'CANCELADO' }
]

const empresaOptions = computed(() => [
  { label: 'Todas as operadoras', value: 'Todas' },
  ...empresasStore.empresas.map((e) => ({
    label: e.razaoSocial,
    value: e.cnpj
  }))
])

const columns = [
  { key: 'displayCode', label: 'Código', sortable: true },
  { key: 'objetivoVoo', label: 'Objetivo / Trajeto', sortable: true },
  { key: 'droneCodigo', label: 'Drone', sortable: true },
  { key: 'piloto', label: 'Piloto' },
  { key: 'cliente', label: 'Cliente' },
  { key: 'dataHoraPlanejada', label: 'Decolagem', sortable: true },
  { key: 'statusVoo', label: 'Status', sortable: true },
  { key: 'acoes', label: 'Ações' }
]

const filteredOperacoes = computed(() => {
  const localQ = debouncedSearch.value.trim().toLowerCase()
  const globalQ = filtersStore.globalSearch.trim().toLowerCase()
  const query = localQ || globalQ

  return operacoesStore.operacoes.filter((op) => {
    if (filtersStore.operationStatus !== 'TODOS' && op.statusVoo !== filtersStore.operationStatus) {
      return false
    }
    if (selectedEmpresa.value !== 'Todas' && op.empresaOpeCnpj !== selectedEmpresa.value) {
      return false
    }
    if (!query) return true

    const resolved = operacoesStore.resolveLabels(op)
    const haystack = [
      op.displayCode,
      String(op.codigo),
      op.objetivoVoo,
      op.origem,
      op.destino,
      op.descricaoArea,
      op.droneCodigo,
      resolved.droneModel,
      resolved.pilotName,
      resolved.clientName
    ]
      .join(' ')
      .toLowerCase()

    return haystack.includes(query)
  })
})

const paginatedOperacoes = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredOperacoes.value.slice(start, start + pageSize.value)
})

watch([debouncedSearch, () => filtersStore.operationStatus, selectedEmpresa], () => {
  page.value = 1
})

async function handleStatusTransition(op: OperacaoVooRecord, nextStatus: StatusVoo) {
  try {
    const updated = await operacoesStore.updateStatus(op.codigo, nextStatus)
    toast.add({
      title: `Voo ${updated.displayCode} atualizado`,
      description: `Status alterado para ${nextStatus.replace('_', ' ')}.`,
      icon: 'i-lucide-navigation',
      color: nextStatus === 'CANCELADO' ? 'red' : 'green'
    })
  } catch (err) {
    toast.add({
      title: 'Bloqueio operacional',
      description: err instanceof Error ? err.message : extractApiErrorMessage(err),
      icon: 'i-lucide-shield-alert',
      color: 'red'
    })
  }
}

onMounted(() => {
  operacoesStore.fetchOperacoes()
  empresasStore.fetchEmpresas()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold text-slate-900 sm:text-2xl">
          Operações de Voo — Agenda & Despacho
        </h1>
        <p class="mt-1 text-sm text-slate-500">
          Planejamento de missões, controle de rotas origem-destino e prevenção de choques de horário.
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-2.5">
        <div class="inline-flex rounded-lg border border-slate-200 bg-white p-0.5 shadow-sm">
          <button
            type="button"
            class="inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-semibold transition-colors"
            :class="viewMode === 'table' ? 'bg-blue-600 text-white' : 'text-slate-600 hover:text-slate-900'"
            @click="viewMode = 'table'"
          >
            <UIcon name="i-lucide-table-2" class="h-3.5 w-3.5" />
            <span>Tabela</span>
          </button>
          <button
            type="button"
            class="inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-semibold transition-colors"
            :class="viewMode === 'calendar' ? 'bg-blue-600 text-white' : 'text-slate-600 hover:text-slate-900'"
            @click="viewMode = 'calendar'"
          >
            <UIcon name="i-lucide-calendar-days" class="h-3.5 w-3.5" />
            <span>Calendário</span>
          </button>
        </div>

        <UButton to="/operacoes/agendar" color="blue" icon="i-lucide-plus">
          Agendar Missão
        </UButton>
      </div>
    </div>

    <!-- KPIs de Operações -->
    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <CommonStatCard
        label="Agendadas"
        :value="operacoesStore.stats.AGENDADO"
        meta="Prontas para despacho"
        tone="blue"
      />
      <CommonStatCard
        label="Em Andamento"
        :value="operacoesStore.stats.EM_ANDAMENTO"
        meta="Em operação ativa"
        tone="amber"
      />
      <CommonStatCard
        label="Concluídas"
        :value="operacoesStore.stats.CONCLUIDO"
        meta="Missões encerradas"
        tone="green"
      />
      <CommonStatCard
        label="Canceladas"
        :value="operacoesStore.stats.CANCELADO"
        meta="Interrompidas / abortadas"
        tone="red"
      />
    </div>

    <!-- Painel Tabela / Calendário -->
    <section class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div class="flex flex-wrap items-end justify-between gap-4 border-b border-slate-200 px-5 py-4">
        <div>
          <h2 class="text-base font-semibold text-slate-900">
            {{ viewMode === 'table' ? 'Registro Completo de Missões' : 'Agenda Visual de Missões' }}
          </h2>
          <p class="text-xs text-slate-500">
            {{ filteredOperacoes.length }} operação(ões) correspondente(s) aos filtros ativos.
          </p>
        </div>

        <div class="flex flex-wrap items-end gap-2.5">
          <div class="flex flex-col gap-1">
            <label for="ops-search" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Buscar Missão
            </label>
            <UInput
              id="ops-search"
              v-model="search"
              size="sm"
              icon="i-lucide-search"
              placeholder="Código, trajeto, piloto..."
              class="w-52"
            />
          </div>

          <div class="flex flex-col gap-1">
            <label for="ops-status" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Status
            </label>
            <USelect
              id="ops-status"
              v-model="filtersStore.operationStatus"
              :options="statusOptions"
              option-attribute="label"
              value-attribute="value"
              size="sm"
            />
          </div>

          <div v-if="authStore.isAdmin" class="flex flex-col gap-1">
            <label for="ops-empresa" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Operadora
            </label>
            <USelect
              id="ops-empresa"
              v-model="selectedEmpresa"
              :options="empresaOptions"
              option-attribute="label"
              value-attribute="value"
              size="sm"
            />
          </div>
        </div>
      </div>

      <!-- Visão Tabela -->
      <template v-if="viewMode === 'table'">
        <UTable
          :rows="paginatedOperacoes"
          :columns="columns"
          :loading="operacoesStore.loading"
          :empty-state="{ icon: 'i-lucide-navigation', label: 'Nenhuma operação de voo encontrada.' }"
          :ui="{
            th: {
              base: 'code-mono uppercase tracking-wider text-[11px] bg-slate-50 text-slate-500'
            },
            td: {
              base: 'text-sm text-slate-800'
            }
          }"
        >
          <template #displayCode-data="{ row }">
            <NuxtLink
              :to="`/operacoes/${row.codigo}`"
              class="code-mono font-semibold text-blue-600 hover:underline"
            >
              {{ row.displayCode }}
            </NuxtLink>
          </template>

          <template #objetivoVoo-data="{ row }">
            <div class="max-w-[260px]">
              <span class="block truncate font-semibold text-slate-900">{{ row.objetivoVoo }}</span>
              <span class="block truncate text-xs text-slate-500">
                {{ row.origem }} → {{ row.destino }}
              </span>
            </div>
          </template>

          <template #droneCodigo-data="{ row }">
            <div>
              <span class="code-mono text-xs font-semibold text-slate-900">{{ row.droneCodigo }}</span>
              <span class="block text-xs text-slate-500">{{ operacoesStore.resolveLabels(row).droneModel }}</span>
            </div>
          </template>

          <template #piloto-data="{ row }">
            <span class="font-medium text-slate-800">{{ operacoesStore.resolveLabels(row).pilotName }}</span>
          </template>

          <template #cliente-data="{ row }">
            <span class="text-xs font-medium text-slate-700">{{ operacoesStore.resolveLabels(row).clientName }}</span>
          </template>

          <template #dataHoraPlanejada-data="{ row }">
            <div>
              <span class="code-mono block text-xs font-medium text-slate-800">
                {{ formatDateTimeBR(row.dataHoraPlanejada) }}
              </span>
              <span class="code-mono block text-[11px] text-slate-400">
                {{ row.duracaoEstimadaMin }} min · {{ row.tipoLocal }}
              </span>
            </div>
          </template>

          <template #statusVoo-data="{ row }">
            <CommonStatusBadge :status="row.statusVoo" />
          </template>

          <template #acoes-data="{ row }">
            <div class="flex items-center gap-1">
              <UButton
                :to="`/operacoes/${row.codigo}`"
                color="gray"
                variant="ghost"
                size="xs"
                icon="i-lucide-eye"
              >
                Ficha
              </UButton>
              <UButton
                v-if="row.statusVoo === 'AGENDADO'"
                color="green"
                variant="ghost"
                size="xs"
                icon="i-lucide-play"
                title="Iniciar voo"
                @click="handleStatusTransition(row, 'EM_ANDAMENTO')"
              />
              <UButton
                v-if="row.statusVoo === 'EM_ANDAMENTO'"
                color="blue"
                variant="ghost"
                size="xs"
                icon="i-lucide-check-circle-2"
                title="Concluir voo"
                @click="handleStatusTransition(row, 'CONCLUIDO')"
              />
            </div>
          </template>
        </UTable>

        <div class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 bg-slate-50 px-5 py-3 text-xs text-slate-600">
          <span>
            Exibindo <strong>{{ paginatedOperacoes.length }}</strong> de <strong>{{ filteredOperacoes.length }}</strong> missões
          </span>
          <UPagination
            v-model="page"
            :page-count="pageSize"
            :total="filteredOperacoes.length"
            size="sm"
          />
        </div>
      </template>

      <!-- Visão Calendário -->
      <CalendarOperacoesCalendar v-else :operacoes="filteredOperacoes" />
    </section>
  </div>
</template>
