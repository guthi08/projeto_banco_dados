<script setup lang="ts">
import { AUDITORIA_CATEGORIAS, GRAVIDADE_OPTIONS } from '~/schemas/auditoria.schema'
import type { AuditoriaRecord } from '~/stores/auditorias'

definePageMeta({
  layout: 'default',
  middleware: 'auth'
})

useHead({
  title: 'Auditoria Técnica & Ocorrências — DRONE-MONITOR'
})

const authStore = useAuthStore()
const auditoriasStore = useAuditoriasStore()
const empresasStore = useEmpresasStore()
const filtersStore = useFiltersStore()
const { formatDateTimeBR } = useDateHelper()

const { search, debouncedSearch } = useDebounceSearch('', 250)
const selectedSeverity = ref<string>('Todas')
const selectedCategory = ref<string>('Todas')
const selectedEmpresa = ref<string>('Todas')

const page = ref(1)
const pageSize = ref(8)

const createModalOpen = ref(false)
const viewingAudit = ref<AuditoriaRecord | null>(null)

const severityOptions = [
  { label: 'Todas as gravidades', value: 'Todas' },
  ...GRAVIDADE_OPTIONS
]

const categoryOptions = ['Todas', ...AUDITORIA_CATEGORIAS]

const empresaOptions = computed(() => [
  { label: 'Todas as operadoras', value: 'Todas' },
  ...empresasStore.empresas.map((e) => ({
    label: e.razaoSocial,
    value: e.cnpj
  }))
])

const onlyDigits = (val: string) => val.replace(/\D/g, '')

const columns = computed(() => {
  const cols = [
    { key: 'displayId', label: 'Registro', sortable: true },
    { key: 'operacaoDisplayCode', label: 'Operação de Voo', sortable: true },
    { key: 'categoria', label: 'Categoria', sortable: true },
    { key: 'descricao', label: 'Descrição Detalhada' },
    { key: 'pilotoNome', label: 'Piloto', sortable: true },
    { key: 'dataHoraEvento', label: 'Data / Hora', sortable: true },
    { key: 'nivelGravidade', label: 'Gravidade', sortable: true },
    { key: 'acoes', label: 'Ações' }
  ]

  if (authStore.isAdmin) {
    cols.splice(2, 0, { key: 'empresaRazaoSocial', label: 'Empresa', sortable: true })
  }

  return cols
})

const filteredAuditorias = computed(() => {
  const localQ = debouncedSearch.value.trim().toLowerCase()
  const globalQ = filtersStore.globalSearch.trim().toLowerCase()
  const query = localQ || globalQ

  return auditoriasStore.auditorias.filter((audit) => {
    if (selectedSeverity.value !== 'Todas' && audit.nivelGravidade !== selectedSeverity.value) {
      return false
    }
    if (selectedCategory.value !== 'Todas' && audit.categoria !== selectedCategory.value) {
      return false
    }
    if (
      authStore.isAdmin &&
      selectedEmpresa.value !== 'Todas' &&
      onlyDigits(audit.empresaOpeCnpj) !== onlyDigits(selectedEmpresa.value)
    ) {
      return false
    }
    if (!query) return true

    const haystack = [
      audit.displayId,
      audit.operacaoDisplayCode,
      audit.empresaRazaoSocial,
      audit.empresaOpeCnpj,
      audit.categoria,
      audit.descricao,
      audit.pilotoNome,
      audit.nivelGravidade
    ]
      .join(' ')
      .toLowerCase()

    return haystack.includes(query)
  })
})

const paginatedAuditorias = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredAuditorias.value.slice(start, start + pageSize.value)
})

watch([debouncedSearch, selectedSeverity, selectedCategory, selectedEmpresa], () => {
  page.value = 1
})

onMounted(() => {
  empresasStore.fetchEmpresas()
  auditoriasStore.fetchAuditorias()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Cabeçalho -->
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold text-slate-900 sm:text-2xl">
          Auditoria Técnica & Ocorrências
        </h1>
        <p class="mt-1 text-sm text-slate-500">
          Fila de conformidade, incidentes operacionais imutáveis e análise por gravidade e categoria.
        </p>
      </div>

      <div class="flex items-center gap-2.5">
        <UButton
          color="gray"
          variant="outline"
          icon="i-lucide-refresh-cw"
          :loading="auditoriasStore.loading"
          @click="auditoriasStore.fetchAuditorias(true)"
        >
          Atualizar
        </UButton>
        <UButton color="blue" icon="i-lucide-plus" @click="createModalOpen = true">
          Nova Ocorrência
        </UButton>
      </div>
    </div>

    <!-- KPIs de Auditoria -->
    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <CommonStatCard
        label="Total de Auditorias"
        :value="auditoriasStore.stats.total"
        meta="Registros imutáveis"
        tone="blue"
      />
      <CommonStatCard
        label="Nível Crítico"
        :value="auditoriasStore.stats.CRITICO"
        meta="Atenção regulatória imediata"
        tone="red"
        icon="i-lucide-octagon-alert"
      />
      <CommonStatCard
        label="Nível Alto / Médio"
        :value="auditoriasStore.stats.ALTO + auditoriasStore.stats.MEDIO"
        meta="Em acompanhamento técnico"
        tone="amber"
      />
      <CommonStatCard
        label="Nível Baixo"
        :value="auditoriasStore.stats.BAIXO"
        meta="Rotina e conformidade"
        tone="green"
      />
    </div>

    <!-- Gráfico Chart.js + Resumo de Severidade -->
    <div class="grid gap-6 lg:grid-cols-12">
      <div class="lg:col-span-8">
        <ChartsOcorrenciasChart :categories="auditoriasStore.byCategory" />
      </div>

      <section class="flex flex-col justify-between rounded-xl border border-slate-200 bg-white p-5 shadow-sm lg:col-span-4">
        <div>
          <h3 class="text-sm font-semibold text-slate-900">
            Matriz de Severidade
          </h3>
          <p class="text-xs text-slate-500">
            Distribuição oficial das ocorrências registradas
          </p>

          <div class="mt-4 space-y-2.5">
            <div class="flex items-center justify-between rounded-lg border border-red-200 bg-red-50 px-3 py-2.5">
              <CommonSeverityBadge severity="CRITICO" />
              <span class="code-mono text-sm font-bold text-red-900">{{ auditoriasStore.stats.CRITICO }}</span>
            </div>
            <div class="flex items-center justify-between rounded-lg border border-orange-200 bg-orange-50 px-3 py-2.5">
              <CommonSeverityBadge severity="ALTO" />
              <span class="code-mono text-sm font-bold text-orange-900">{{ auditoriasStore.stats.ALTO }}</span>
            </div>
            <div class="flex items-center justify-between rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5">
              <CommonSeverityBadge severity="MEDIO" />
              <span class="code-mono text-sm font-bold text-amber-900">{{ auditoriasStore.stats.MEDIO }}</span>
            </div>
            <div class="flex items-center justify-between rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5">
              <CommonSeverityBadge severity="BAIXO" />
              <span class="code-mono text-sm font-bold text-slate-800">{{ auditoriasStore.stats.BAIXO }}</span>
            </div>
          </div>
        </div>

        <p class="mt-4 border-t border-slate-200 pt-3 text-[11px] text-slate-500">
          Cada auditoria pertence exclusivamente a uma operação de voo e é preservada de forma imutável.
        </p>
      </section>
    </div>

    <!-- Tabela de Ocorrências -->
    <section class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div class="flex flex-wrap items-end justify-between gap-4 border-b border-slate-200 px-5 py-4">
        <div>
          <h2 class="text-base font-semibold text-slate-900">
            Histórico de Ocorrências Técnicas
          </h2>
          <p class="text-xs text-slate-500">
            Filtre por gravidade, categoria ou código da operação.
          </p>
        </div>

        <div class="flex flex-wrap items-end gap-2.5">
          <div class="flex flex-col gap-1">
            <label for="aud-search" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Buscar Registro
            </label>
            <UInput
              id="aud-search"
              v-model="search"
              size="sm"
              icon="i-lucide-search"
              placeholder="Código, voo, descrição..."
              class="w-52"
            />
          </div>

          <div class="flex flex-col gap-1">
            <label for="aud-severity" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Gravidade
            </label>
            <USelect
              id="aud-severity"
              v-model="selectedSeverity"
              :options="severityOptions"
              option-attribute="label"
              value-attribute="value"
              size="sm"
            />
          </div>

          <div class="flex flex-col gap-1">
            <label for="aud-category" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Categoria
            </label>
            <USelect
              id="aud-category"
              v-model="selectedCategory"
              :options="categoryOptions"
              size="sm"
            />
          </div>

          <div v-if="authStore.isAdmin" class="flex flex-col gap-1">
            <label for="aud-empresa" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Empresa
            </label>
            <USelect
              id="aud-empresa"
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
        :rows="paginatedAuditorias"
        :columns="columns"
        :loading="auditoriasStore.loading"
        :empty-state="{ icon: 'i-lucide-shield-check', label: 'Nenhuma auditoria encontrada para os filtros aplicados.' }"
        :ui="{
          th: {
            base: 'code-mono uppercase tracking-wider text-[11px] bg-slate-50 text-slate-500'
          },
          td: {
            base: 'text-sm text-slate-800'
          }
        }"
      >
        <template #displayId-data="{ row }">
          <span class="code-mono text-xs font-semibold text-slate-900">{{ row.displayId }}</span>
        </template>

        <template #operacaoDisplayCode-data="{ row }">
          <NuxtLink
            :to="`/operacoes/${row.operacaoVooCodigo}`"
            class="code-mono text-xs font-semibold text-blue-600 hover:underline"
          >
            {{ row.operacaoDisplayCode }}
          </NuxtLink>
        </template>

        <template #empresaRazaoSocial-data="{ row }">
          <div>
            <span class="block text-xs font-semibold text-slate-900">{{ row.empresaRazaoSocial }}</span>
            <span v-if="row.empresaOpeCnpj" class="code-mono block text-[11px] text-slate-500">
              {{ row.empresaOpeCnpj }}
            </span>
          </div>
        </template>

        <template #categoria-data="{ row }">
          <span class="font-semibold text-slate-900">{{ row.categoria }}</span>
        </template>

        <template #descricao-data="{ row }">
          <p class="max-w-[280px] truncate text-xs text-slate-600" :title="row.descricao">
            {{ row.descricao }}
          </p>
        </template>

        <template #pilotoNome-data="{ row }">
          <span class="text-xs font-medium text-slate-800">{{ row.pilotoNome }}</span>
        </template>

        <template #dataHoraEvento-data="{ row }">
          <span class="code-mono text-xs text-slate-600">{{ formatDateTimeBR(row.dataHoraEvento) }}</span>
        </template>

        <template #nivelGravidade-data="{ row }">
          <CommonSeverityBadge :severity="row.nivelGravidade" />
        </template>

        <template #acoes-data="{ row }">
          <UButton
            color="gray"
            variant="ghost"
            size="xs"
            icon="i-lucide-eye"
            @click="viewingAudit = row"
          >
            Detalhes
          </UButton>
        </template>
      </UTable>

      <div class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 bg-slate-50 px-5 py-3 text-xs text-slate-600">
        <span>
          Exibindo <strong>{{ paginatedAuditorias.length }}</strong> de <strong>{{ filteredAuditorias.length }}</strong> auditorias
        </span>
        <UPagination
          v-model="page"
          :page-count="pageSize"
          :total="filteredAuditorias.length"
          size="sm"
        />
      </div>
    </section>

    <!-- Modal de Nova Auditoria -->
    <FormsAuditoriaFormModal v-model="createModalOpen" />

    <!-- Modal Detalhe da Auditoria -->
    <UModal :model-value="Boolean(viewingAudit)" @update:model-value="!$event && (viewingAudit = null)">
      <UCard v-if="viewingAudit">
        <template #header>
          <div class="flex items-start justify-between gap-3">
            <div>
              <div class="flex items-center gap-2">
                <span class="code-mono text-xs font-bold text-slate-700">{{ viewingAudit.displayId }}</span>
                <CommonSeverityBadge :severity="viewingAudit.nivelGravidade" />
              </div>
              <h3 class="mt-1 text-base font-semibold text-slate-900">
                {{ viewingAudit.categoria }}
              </h3>
            </div>
            <UButton
              color="gray"
              variant="ghost"
              icon="i-lucide-x"
              size="sm"
              @click="viewingAudit = null"
            />
          </div>
        </template>

        <div class="space-y-4">
          <div class="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 sm:grid-cols-2">
            <div>
              <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Operação de Voo</span>
              <NuxtLink
                :to="`/operacoes/${viewingAudit.operacaoVooCodigo}`"
                class="code-mono text-sm font-semibold text-blue-600 hover:underline"
              >
                {{ viewingAudit.operacaoDisplayCode }}
              </NuxtLink>
            </div>
            <div>
              <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Data / Hora do Evento</span>
              <span class="code-mono text-sm font-semibold text-slate-900">
                {{ formatDateTimeBR(viewingAudit.dataHoraEvento) }}
              </span>
            </div>
            <div>
              <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Piloto Responsável</span>
              <span class="text-sm font-semibold text-slate-900">{{ viewingAudit.pilotoNome }}</span>
            </div>
            <div>
              <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Empresa Operadora</span>
              <span class="block text-sm font-semibold text-slate-900">{{ viewingAudit.empresaRazaoSocial }}</span>
              <span v-if="viewingAudit.empresaOpeCnpj" class="code-mono text-[11px] text-slate-500">
                {{ viewingAudit.empresaOpeCnpj }}
              </span>
            </div>
          </div>

          <div>
            <span class="code-mono mb-1 block text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Relato Técnico
            </span>
            <p class="rounded-lg border border-slate-200 bg-white p-3.5 text-sm leading-relaxed text-slate-700">
              {{ viewingAudit.descricao }}
            </p>
          </div>
        </div>

        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="gray" variant="outline" @click="viewingAudit = null">
              Fechar
            </UButton>
            <UButton :to="`/operacoes/${viewingAudit.operacaoVooCodigo}`" color="blue" icon="i-lucide-external-link">
              Abrir Ficha do Voo
            </UButton>
          </div>
        </template>
      </UCard>
    </UModal>
  </div>
</template>
