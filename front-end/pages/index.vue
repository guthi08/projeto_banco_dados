<script setup lang="ts">
import type { DashboardAuditItem, DashboardOperation } from '~/composables/useDashboardData'
import type { PeriodFilter, StatusFilterValue } from '~/stores/filters'

definePageMeta({
  layout: 'default',
  middleware: 'auth'
})

useHead({
  title: 'Painel de Monitoramento de Drone — DRONE-MONITOR'
})

const authStore = useAuthStore()
const filtersStore = useFiltersStore()
const empresasStore = useEmpresasStore()
const { formatDateBR, formatDateTimeBR } = useDateHelper()
const {
  loading,
  periodOperations,
  audits,
  validityAlerts,
  statusCounts,
  monthlySeries,
  kpis,
  refresh
} = useDashboardData()

const { search: tableSearch, debouncedSearch: debouncedTableSearch } = useDebounceSearch('', 250)
const showAllRows = ref(false)
const selectedOperation = ref<DashboardOperation | null>(null)
const selectedAudit = ref<DashboardAuditItem | null>(null)

const periodOptions: { label: string; value: PeriodFilter }[] = [
  { label: 'Últimos 7 dias', value: '7d' },
  { label: 'Últimos 30 dias', value: '30d' },
  { label: 'Últimos 90 dias', value: '90d' },
  { label: 'Todo o período', value: 'all' }
]

const statusOptions: { label: string; value: StatusFilterValue }[] = [
  { label: 'Todos os status', value: 'TODOS' },
  { label: 'Agendado', value: 'AGENDADO' },
  { label: 'Em Andamento', value: 'EM_ANDAMENTO' },
  { label: 'Concluído', value: 'CONCLUIDO' },
  { label: 'Cancelado', value: 'CANCELADO' }
]

const filteredOperations = computed(() => {
  const localQ = debouncedTableSearch.value.trim().toLowerCase()
  const globalQ = filtersStore.globalSearch.trim().toLowerCase()
  const query = localQ || globalQ

  return periodOperations.value.filter((op) => {
    const matchesStatus =
      filtersStore.operationStatus === 'TODOS' || op.status === filtersStore.operationStatus

    if (!matchesStatus) return false
    if (!query) return true

    const haystack = [
      op.code,
      op.droneCode,
      op.droneModel,
      op.pilotName,
      op.clientName,
      op.objective,
      op.originDesc,
      op.destDesc
    ]
      .join(' ')
      .toLowerCase()

    return haystack.includes(query)
  })
})

const visibleOperations = computed(() =>
  showAllRows.value ? filteredOperations.value : filteredOperations.value.slice(0, 5)
)

const todayLabel = computed(() => formatDateBR(new Date().toISOString()))

onMounted(() => {
  refresh()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Cabeçalho do Painel + Filtro de Período (Pinia) -->
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold text-slate-900 sm:text-2xl">
          Painel de Monitoramento de Drone
        </h1>
        <p class="mt-1 text-sm text-slate-500">
          Operadora ativa: <strong class="text-slate-800">{{ empresasStore.activeOperator?.razaoSocial || authStore.operatorName }}</strong> · Validade de frota e conformidade ANAC.
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-2.5">
        <span class="code-mono hidden text-xs font-semibold text-slate-500 md:inline">
          Ref.: {{ todayLabel }}
        </span>

        <USelect
          v-model="filtersStore.period"
          :options="periodOptions"
          option-attribute="label"
          value-attribute="value"
          size="sm"
          icon="i-lucide-calendar"
          aria-label="Filtrar por período"
        />

        <UButton
          color="gray"
          variant="outline"
          size="sm"
          icon="i-lucide-refresh-cw"
          :loading="loading"
          @click="refresh"
        >
          Atualizar
        </UButton>
      </div>
    </div>

    <!-- Linha de KPIs (4 Cards Carbon AERO-MONITOR) -->
    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <CommonStatCard
        label="Voos em Andamento"
        :value="kpis.activeFlights"
        meta="Em operação ativa"
        tone="green"
      />
      <CommonStatCard
        label="Drones Disponíveis"
        :value="`${kpis.availableDrones}/${kpis.totalDrones}`"
        :meta="`${kpis.availablePct}% aptos ANAC`"
        tone="blue"
      />
      <CommonStatCard
        label="Alertas CMA / ANAC"
        :value="kpis.alertsCount"
        :meta="`${kpis.criticalAlertsCount} bloqueios críticos`"
        tone="red"
        icon="i-lucide-alert-triangle"
      />
      <CommonStatCard
        label="Auditorias & Incidentes"
        :value="kpis.auditsCount"
        :meta="`${kpis.criticalAudits} altos ou críticos`"
        tone="amber"
      />
    </div>

    <!-- Linha Analítica (Chart.js: Doughnut + Barras) -->
    <div class="grid gap-6 lg:grid-cols-2">
      <ChartsStatusChart :counts="statusCounts" />
      <ChartsMonthlyFlightsChart :series="monthlySeries" />
    </div>

    <!-- Grid Assimétrico Principal (Tabela de Operações + Coluna de Compliance) -->
    <div class="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_340px]">
      <!-- Painel Central: Operações de Voo Recentes -->
      <section class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <div class="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 px-5 py-4">
          <div>
            <h2 class="text-base font-semibold text-slate-900">
              Operações de Voo Recentes
            </h2>
            <p class="text-xs text-slate-500">
              Acompanhamento de missões alocadas por aeronave, piloto e cliente
            </p>
          </div>

          <div class="flex flex-wrap items-end gap-2.5">
            <div class="flex flex-col gap-1">
              <label for="dash-op-search" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
                Filtrar
              </label>
              <UInput
                id="dash-op-search"
                v-model="tableSearch"
                size="sm"
                icon="i-lucide-search"
                placeholder="Código, piloto, cliente..."
                class="w-48 sm:w-52"
              />
            </div>

            <div class="flex flex-col gap-1">
              <label for="dash-op-status" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
                Status
              </label>
              <USelect
                id="dash-op-status"
                v-model="filtersStore.operationStatus"
                :options="statusOptions"
                option-attribute="label"
                value-attribute="value"
                size="sm"
              />
            </div>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full border-collapse text-left">
            <thead>
              <tr class="border-b border-slate-200 bg-slate-50">
                <th class="code-mono whitespace-nowrap px-4 py-3 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                  Código
                </th>
                <th class="code-mono whitespace-nowrap px-4 py-3 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                  Drone
                </th>
                <th class="code-mono whitespace-nowrap px-4 py-3 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                  Piloto
                </th>
                <th class="code-mono whitespace-nowrap px-4 py-3 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                  Cliente
                </th>
                <th class="code-mono whitespace-nowrap px-4 py-3 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                  Status
                </th>
                <th class="code-mono whitespace-nowrap px-4 py-3 text-right text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                  Ações
                </th>
              </tr>
            </thead>

            <tbody class="divide-y divide-slate-200 text-sm">
              <tr v-if="visibleOperations.length === 0">
                <td colspan="6" class="px-4 py-10 text-center text-sm text-slate-500">
                  Nenhuma operação encontrada para os filtros aplicados.
                </td>
              </tr>

              <tr
                v-for="op in visibleOperations"
                :key="op.code"
                class="transition-colors hover:bg-slate-50"
              >
                <td class="whitespace-nowrap px-4 py-3.5">
                  <span class="code-mono text-xs font-semibold text-slate-900">{{ op.code }}</span>
                  <span class="block text-xs text-slate-500">{{ formatDateTimeBR(op.plannedAt) }}</span>
                </td>
                <td class="whitespace-nowrap px-4 py-3.5">
                  <span class="code-mono text-xs font-semibold text-slate-800">{{ op.droneCode }}</span>
                  <span class="block text-xs text-slate-500">{{ op.droneModel }}</span>
                </td>
                <td class="px-4 py-3.5">
                  <span class="font-medium text-slate-900">{{ op.pilotName }}</span>
                </td>
                <td class="px-4 py-3.5">
                  <span class="text-slate-700">{{ op.clientName }}</span>
                </td>
                <td class="whitespace-nowrap px-4 py-3.5">
                  <CommonStatusBadge :status="op.status" />
                </td>
                <td class="whitespace-nowrap px-4 py-3.5 text-right">
                  <UButton
                    color="gray"
                    variant="ghost"
                    size="xs"
                    icon="i-lucide-eye"
                    @click="selectedOperation = op"
                  >
                    Detalhes
                  </UButton>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex flex-wrap items-center justify-between gap-2 border-t border-slate-200 bg-slate-50 px-5 py-3 text-xs text-slate-600">
          <span>
            Exibindo <strong>{{ visibleOperations.length }}</strong> de <strong>{{ filteredOperations.length }}</strong> operações filtradas
          </span>
          <div class="flex items-center gap-3">
            <button
              v-if="filteredOperations.length > 5"
              type="button"
              class="font-semibold text-blue-600 hover:underline"
              @click="showAllRows = !showAllRows"
            >
              {{ showAllRows ? 'Mostrar resumo' : 'Ver todas as operações' }}
            </button>
            <NuxtLink to="/operacoes" class="font-semibold text-blue-600 hover:underline">
              Gerenciar em tela cheia
            </NuxtLink>
          </div>
        </div>
      </section>

      <!-- Coluna Direita: Compliance (Alertas de Validade + Últimas Auditorias) -->
      <aside class="space-y-6">
        <!-- Alertas de Validade ANAC / CMA -->
        <section class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div class="flex items-center justify-between border-b border-slate-200 px-5 py-3.5">
            <h2 class="text-sm font-semibold text-slate-900">
              Alertas de Validade
            </h2>
            <UBadge :color="validityAlerts.length > 0 ? 'red' : 'green'" variant="subtle" size="xs" class="code-mono">
              {{ validityAlerts.length }}
            </UBadge>
          </div>

          <div class="space-y-2.5 p-4">
            <div v-if="validityAlerts.length === 0" class="rounded-lg bg-emerald-50 p-3 text-center text-xs font-medium text-emerald-800">
              Frota e pilotos 100% regulares junto à ANAC/CMA.
            </div>

            <NuxtLink
              v-for="alert in validityAlerts"
              :key="alert.id"
              :to="alert.route"
              class="flex items-start gap-2.5 rounded-lg border-l-4 p-3 transition-all hover:brightness-95"
              :class="
                alert.level === 'critical'
                  ? 'border-red-500 bg-red-50 text-red-900'
                  : 'border-amber-500 bg-amber-50 text-amber-900'
              "
            >
              <UIcon
                :name="alert.level === 'critical' ? 'i-lucide-shield-x' : 'i-lucide-clock-alert'"
                class="mt-0.5 h-4 w-4 shrink-0"
              />
              <div class="min-w-0">
                <p class="text-xs font-semibold leading-snug">
                  {{ alert.title }}
                </p>
                <p class="mt-0.5 text-[11px] opacity-90">
                  {{ alert.subtitle }}
                </p>
              </div>
            </NuxtLink>
          </div>
        </section>

        <!-- Últimas Auditorias -->
        <section class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div class="flex items-center justify-between border-b border-slate-200 px-5 py-3.5">
            <h2 class="text-sm font-semibold text-slate-900">
              Últimas Auditorias
            </h2>
            <NuxtLink to="/auditorias" class="text-xs font-semibold text-blue-600 hover:underline">
              + Registrar
            </NuxtLink>
          </div>

          <div class="space-y-2.5 p-4">
            <button
              v-for="audit in audits.slice(0, 4)"
              :key="audit.id"
              type="button"
              class="flex w-full items-center justify-between gap-2.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-left transition-colors hover:bg-slate-100"
              @click="selectedAudit = audit"
            >
              <div class="min-w-0">
                <span class="block truncate text-xs font-semibold text-slate-900">
                  {{ audit.category }}
                </span>
                <span class="code-mono block text-[11px] text-slate-500">
                  {{ audit.operationCode }} · {{ audit.pilotName }}
                </span>
              </div>
              <CommonSeverityBadge :severity="audit.severity" />
            </button>
          </div>

          <div class="flex items-center justify-between border-t border-slate-200 bg-slate-50 px-4 py-2.5 text-xs text-slate-500">
            <span>Controle de qualidade</span>
            <NuxtLink to="/auditorias" class="font-semibold text-blue-600 hover:underline">
              Abrir Auditoria
            </NuxtLink>
          </div>
        </section>
      </aside>
    </div>

    <!-- Modal Detalhe Rápido da Operação -->
    <UModal :model-value="Boolean(selectedOperation)" @update:model-value="!$event && (selectedOperation = null)">
      <UCard v-if="selectedOperation">
        <template #header>
          <div class="flex items-center justify-between gap-2">
            <div>
              <span class="code-mono text-xs font-semibold text-blue-600">
                {{ selectedOperation.code }}
              </span>
              <h3 class="text-base font-semibold text-slate-900">
                {{ selectedOperation.objective }}
              </h3>
            </div>
            <CommonStatusBadge :status="selectedOperation.status" />
          </div>
        </template>

        <div class="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 sm:grid-cols-2">
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Drone</span>
            <span class="text-sm font-semibold text-slate-900">
              {{ selectedOperation.droneCode }} · {{ selectedOperation.droneModel }}
            </span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Piloto Responsável</span>
            <span class="text-sm font-semibold text-slate-900">{{ selectedOperation.pilotName }}</span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Cliente Solicitante</span>
            <span class="text-sm font-semibold text-slate-900">{{ selectedOperation.clientName }}</span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Decolagem Planejada</span>
            <span class="code-mono text-sm font-semibold text-slate-900">
              {{ formatDateTimeBR(selectedOperation.plannedAt) }}
            </span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Origem</span>
            <span class="text-sm text-slate-800">{{ selectedOperation.originDesc }}</span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Destino</span>
            <span class="text-sm text-slate-800">{{ selectedOperation.destDesc }}</span>
          </div>
        </div>

        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="gray" variant="outline" @click="selectedOperation = null">
              Fechar
            </UButton>
            <UButton :to="`/operacoes/${selectedOperation.codigo}`" color="blue" icon="i-lucide-external-link">
              Abrir Ficha do Voo
            </UButton>
          </div>
        </template>
      </UCard>
    </UModal>

    <!-- Modal Detalhe Rápido da Auditoria -->
    <UModal :model-value="Boolean(selectedAudit)" @update:model-value="!$event && (selectedAudit = null)">
      <UCard v-if="selectedAudit">
        <template #header>
          <div class="flex items-center justify-between gap-2">
            <div>
              <span class="code-mono text-xs font-semibold text-slate-500">
                {{ selectedAudit.id }} · Voo {{ selectedAudit.operationCode }}
              </span>
              <h3 class="text-base font-semibold text-slate-900">
                {{ selectedAudit.category }}
              </h3>
            </div>
            <CommonSeverityBadge :severity="selectedAudit.severity" />
          </div>
        </template>

        <div class="space-y-3 text-sm text-slate-700">
          <p class="leading-relaxed">
            {{ selectedAudit.description }}
          </p>
          <div class="code-mono flex flex-wrap gap-4 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-600">
            <span>Piloto: {{ selectedAudit.pilotName }}</span>
            <span>Registro: {{ formatDateTimeBR(selectedAudit.timestamp) }}</span>
          </div>
        </div>

        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="gray" variant="outline" @click="selectedAudit = null">
              Fechar
            </UButton>
            <UButton to="/auditorias" color="blue">
              Ir para Auditorias
            </UButton>
          </div>
        </template>
      </UCard>
    </UModal>
  </div>
</template>
