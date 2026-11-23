<script setup lang="ts">
import type { DroneRecord } from '~/stores/drones'

definePageMeta({
  layout: 'default',
  middleware: 'auth'
})

useHead({
  title: 'Frota de Drones & Controle ANAC — DRONE-MONITOR'
})

const authStore = useAuthStore()
const dronesStore = useDronesStore()
const empresasStore = useEmpresasStore()
const filtersStore = useFiltersStore()
const toast = useToast()
const { formatDateBR, getValidityStatus } = useDateHelper()

const { search, debouncedSearch } = useDebounceSearch('', 250)
const selectedValidity = ref<'Todos' | 'valid' | 'expiring' | 'expired' | 'maintenance'>('Todos')
const selectedUso = ref<'Todos' | 'basico' | 'avancado'>('Todos')
const selectedEmpresa = ref<string>('Todas')

const page = ref(1)
const pageSize = ref(8)

const formModalOpen = ref(false)
const editingDrone = ref<DroneRecord | null>(null)
const inspectingDrone = ref<DroneRecord | null>(null)
const deletingDrone = ref<DroneRecord | null>(null)

const columns = [
  { key: 'codigo', label: 'Código ANAC', sortable: true },
  { key: 'modelo', label: 'Fabricante / Modelo', sortable: true },
  { key: 'numSerie', label: 'Nº de Série' },
  { key: 'pesoMaxDecolagem', label: 'MTOW', sortable: true },
  { key: 'tipoUso', label: 'Uso', sortable: true },
  { key: 'empresa', label: 'Operadora' },
  { key: 'validade', label: 'Validade ANAC', sortable: true },
  { key: 'acoes', label: 'Ações' }
]

const validityOptions = [
  { label: 'Todas as situações', value: 'Todos' },
  { label: '🟢 Válidos (> 30 dias)', value: 'valid' },
  { label: '🟡 Vencendo (< 30 dias)', value: 'expiring' },
  { label: '🔴 Vencidos (Bloqueados)', value: 'expired' },
  { label: '🔧 Em Manutenção', value: 'maintenance' }
]

const usoOptions = [
  { label: 'Todos os usos', value: 'Todos' },
  { label: 'Básico', value: 'basico' },
  { label: 'Avançado', value: 'avancado' }
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

const filteredDrones = computed(() => {
  const localQ = debouncedSearch.value.trim().toLowerCase()
  const globalQ = filtersStore.globalSearch.trim().toLowerCase()
  const query = localQ || globalQ

  return dronesStore.drones.filter((drone) => {
    const level = getValidityStatus(drone.dataValidadeCadastro, 30)

    if (selectedValidity.value === 'maintenance' && !drone.emManutencao) return false
    if (
      selectedValidity.value !== 'Todos' &&
      selectedValidity.value !== 'maintenance' &&
      level !== selectedValidity.value
    ) {
      return false
    }

    if (selectedUso.value !== 'Todos' && drone.tipoUso !== selectedUso.value) return false
    if (selectedEmpresa.value !== 'Todas' && drone.empresaOpeCnpj !== selectedEmpresa.value) return false

    if (!query) return true

    const haystack = [
      drone.codigo,
      drone.fabricante,
      drone.modelo,
      drone.numSerie,
      getOperatorName(drone.empresaOpeCnpj)
    ]
      .join(' ')
      .toLowerCase()

    return haystack.includes(query)
  })
})

const paginatedDrones = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredDrones.value.slice(start, start + pageSize.value)
})

watch([debouncedSearch, selectedValidity, selectedUso, selectedEmpresa], () => {
  page.value = 1
})

function openCreateModal() {
  editingDrone.value = null
  formModalOpen.value = true
}

function openEditModal(drone: DroneRecord) {
  inspectingDrone.value = null
  editingDrone.value = drone
  formModalOpen.value = true
}

async function handleRenewAnac(codigo: string) {
  try {
    const updated = await dronesStore.renewAnacRegistration(codigo, 12)
    inspectingDrone.value = updated
    toast.add({
      title: 'Cadastro ANAC renovado',
      description: `Nova validade para ${updated.codigo}: ${formatDateBR(updated.dataValidadeCadastro)}.`,
      icon: 'i-lucide-shield-check',
      color: 'green'
    })
  } catch (err) {
    toast.add({
      title: 'Falha ao renovar registro',
      description: extractApiErrorMessage(err),
      icon: 'i-lucide-alert-triangle',
      color: 'red'
    })
  }
}

async function handleToggleMaintenance(codigo: string) {
  const updated = await dronesStore.toggleMaintenance(codigo)
  inspectingDrone.value = updated
  toast.add({
    title: updated.emManutencao ? 'Aeronave em manutenção' : 'Aeronave liberada',
    description: `${updated.codigo} ${updated.emManutencao ? 'suspensa para manutenção.' : 'retornou à frota ativa.'}`,
    icon: 'i-lucide-wrench',
    color: updated.emManutencao ? 'amber' : 'green'
  })
}

async function confirmDeleteDrone() {
  if (!deletingDrone.value) return
  const code = deletingDrone.value.codigo
  try {
    await dronesStore.removeDrone(code)
    deletingDrone.value = null
    toast.add({
      title: 'Aeronave removida',
      description: `Drone ${code} foi removido do inventário.`,
      icon: 'i-lucide-trash-2',
      color: 'blue'
    })
  } catch (err) {
    toast.add({
      title: 'Erro ao remover aeronave',
      description: extractApiErrorMessage(err),
      icon: 'i-lucide-alert-triangle',
      color: 'red'
    })
  }
}

onMounted(() => {
  dronesStore.fetchDrones()
  empresasStore.fetchEmpresas()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold text-slate-900 sm:text-2xl">
          Frota de Drones & Controle ANAC
        </h1>
        <p class="mt-1 text-sm text-slate-500">
          Catálogo de aeronaves no padrão brasileiro (PR, PP, PS) com monitoramento automático de validade SISANT/ANAC.
        </p>
      </div>

      <div class="flex items-center gap-2.5">
        <UButton
          color="gray"
          variant="outline"
          icon="i-lucide-refresh-cw"
          :loading="dronesStore.loading"
          @click="dronesStore.fetchDrones(true)"
        >
          Atualizar
        </UButton>
        <UButton color="blue" icon="i-lucide-plus" @click="openCreateModal">
          Novo Drone
        </UButton>
      </div>
    </div>

    <!-- KPIs da Frota -->
    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <CommonStatCard
        label="Frota Cadastrada"
        :value="dronesStore.stats.total"
        :meta="`${dronesStore.stats.disponiveisPct}% aptos para voo`"
        tone="blue"
      />
      <CommonStatCard
        label="ANAC Regular"
        :value="dronesStore.stats.validos"
        meta="Validade acima de 30 dias"
        tone="green"
      />
      <CommonStatCard
        label="Vencendo em < 30 dias"
        :value="dronesStore.stats.vencendo"
        meta="Ação preventiva necessária"
        tone="amber"
        icon="i-lucide-clock-alert"
      />
      <CommonStatCard
        label="Cadastro Expirado"
        :value="dronesStore.stats.vencidos"
        meta="Bloqueados para despacho"
        tone="red"
        icon="i-lucide-shield-x"
      />
    </div>

    <!-- Legenda de Semáforo ANAC -->
    <div class="flex flex-wrap items-center gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 text-xs text-slate-600 shadow-sm">
      <span class="code-mono font-semibold uppercase tracking-wider text-slate-500">Trava ANAC:</span>
      <span class="inline-flex items-center gap-1.5">
        <span class="h-2 w-2 rounded-full bg-emerald-600" />
        <strong>Válido</strong> (liberado)
      </span>
      <span class="inline-flex items-center gap-1.5">
        <span class="h-2 w-2 rounded-full bg-amber-500" />
        <strong>Vence em &lt; 30 dias</strong> (alerta)
      </span>
      <span class="inline-flex items-center gap-1.5">
        <span class="h-2 w-2 rounded-full bg-red-600" />
        <strong>Vencido</strong> (bloqueia seleção no agendamento de voos)
      </span>
    </div>

    <!-- Tabela da Frota -->
    <section class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div class="flex flex-wrap items-end justify-between gap-4 border-b border-slate-200 px-5 py-4">
        <div>
          <h2 class="text-base font-semibold text-slate-900">
            Inventário de Aeronaves
          </h2>
          <p class="text-xs text-slate-500">
            Clique em <strong>Inspecionar</strong> para consultar detalhes e renovar o cadastro ANAC.
          </p>
        </div>

        <div class="flex flex-wrap items-end gap-2.5">
          <div class="flex flex-col gap-1">
            <label for="drone-search" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Buscar Aeronave
            </label>
            <UInput
              id="drone-search"
              v-model="search"
              size="sm"
              icon="i-lucide-search"
              placeholder="PR/PP/PS, modelo, série..."
              class="w-52"
            />
          </div>

          <div class="flex flex-col gap-1">
            <label for="drone-validity" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Situação ANAC
            </label>
            <USelect
              id="drone-validity"
              v-model="selectedValidity"
              :options="validityOptions"
              option-attribute="label"
              value-attribute="value"
              size="sm"
            />
          </div>

          <div class="flex flex-col gap-1">
            <label for="drone-uso" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Uso
            </label>
            <USelect
              id="drone-uso"
              v-model="selectedUso"
              :options="usoOptions"
              option-attribute="label"
              value-attribute="value"
              size="sm"
            />
          </div>

          <div v-if="authStore.isAdmin" class="flex flex-col gap-1">
            <label for="drone-empresa" class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">
              Operadora
            </label>
            <USelect
              id="drone-empresa"
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
        :rows="paginatedDrones"
        :columns="columns"
        :loading="dronesStore.loading"
        :empty-state="{ icon: 'i-lucide-cpu', label: 'Nenhuma aeronave encontrada para os filtros aplicados.' }"
        :ui="{
          th: {
            base: 'code-mono uppercase tracking-wider text-[11px] bg-slate-50 text-slate-500'
          },
          td: {
            base: 'text-sm text-slate-800'
          }
        }"
      >
        <template #codigo-data="{ row }">
          <div class="flex items-center gap-2">
            <span class="code-mono font-semibold text-slate-900">{{ row.codigo }}</span>
            <UBadge v-if="row.emManutencao" color="gray" variant="subtle" size="xs" class="code-mono">
              MANUT.
            </UBadge>
          </div>
        </template>

        <template #modelo-data="{ row }">
          <div>
            <span class="font-semibold text-slate-900">{{ row.modelo }}</span>
            <span class="block text-xs text-slate-500">{{ row.fabricante }}</span>
          </div>
        </template>

        <template #numSerie-data="{ row }">
          <span class="code-mono text-xs text-slate-600">{{ row.numSerie }}</span>
        </template>

        <template #pesoMaxDecolagem-data="{ row }">
          <span class="code-mono text-xs font-medium text-slate-800">
            {{ Number(row.pesoMaxDecolagem).toFixed(2) }} kg
          </span>
        </template>

        <template #tipoUso-data="{ row }">
          <UBadge
            :color="row.tipoUso === 'avancado' ? 'blue' : 'gray'"
            variant="subtle"
            size="xs"
            class="capitalize"
          >
            {{ row.tipoUso === 'avancado' ? 'Avançado' : 'Básico' }}
          </UBadge>
        </template>

        <template #empresa-data="{ row }">
          <span class="block max-w-[180px] truncate text-xs font-medium text-slate-700" :title="getOperatorName(row.empresaOpeCnpj)">
            {{ getOperatorName(row.empresaOpeCnpj) }}
          </span>
        </template>

        <template #validade-data="{ row }">
          <div class="space-y-1">
            <CommonValidityBadge :valid-until="row.dataValidadeCadastro" domain-label="ANAC" />
            <span class="code-mono block text-[11px] text-slate-500">
              {{ formatDateBR(row.dataValidadeCadastro) }}
            </span>
          </div>
        </template>

        <template #acoes-data="{ row }">
          <div class="flex items-center gap-1">
            <UButton
              color="gray"
              variant="ghost"
              size="xs"
              icon="i-lucide-clipboard-check"
              @click="inspectingDrone = row"
            >
              Inspecionar
            </UButton>
            <UButton
              color="gray"
              variant="ghost"
              size="xs"
              icon="i-lucide-pencil"
              aria-label="Editar drone"
              @click="openEditModal(row)"
            />
            <UButton
              color="red"
              variant="ghost"
              size="xs"
              icon="i-lucide-trash-2"
              aria-label="Excluir drone"
              @click="deletingDrone = row"
            />
          </div>
        </template>
      </UTable>

      <div class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 bg-slate-50 px-5 py-3 text-xs text-slate-600">
        <span>
          Exibindo <strong>{{ paginatedDrones.length }}</strong> de <strong>{{ filteredDrones.length }}</strong> aeronaves
        </span>
        <UPagination
          v-model="page"
          :page-count="pageSize"
          :total="filteredDrones.length"
          size="sm"
        />
      </div>
    </section>

    <!-- Modais -->
    <FormsDroneFormModal
      v-model="formModalOpen"
      :editing-drone="editingDrone"
    />

    <CommonDroneInspecaoModal
      :drone="inspectingDrone"
      @close="inspectingDrone = null"
      @edit="openEditModal"
      @renew="handleRenewAnac"
      @toggle-maintenance="handleToggleMaintenance"
    />

    <UModal :model-value="Boolean(deletingDrone)" @update:model-value="!$event && (deletingDrone = null)">
      <UCard v-if="deletingDrone">
        <template #header>
          <div class="flex items-center gap-2 text-red-700">
            <UIcon name="i-lucide-trash-2" class="h-5 w-5" />
            <h3 class="text-base font-semibold">Remover aeronave da frota</h3>
          </div>
        </template>
        <p class="text-sm text-slate-600">
          Confirma a remoção do drone <strong class="code-mono text-slate-900">{{ deletingDrone.codigo }}</strong> ({{ deletingDrone.fabricante }} {{ deletingDrone.modelo }})?
        </p>
        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="gray" variant="outline" @click="deletingDrone = null">Cancelar</UButton>
            <UButton color="red" :loading="dronesStore.loading" @click="confirmDeleteDrone">Remover</UButton>
          </div>
        </template>
      </UCard>
    </UModal>
  </div>
</template>
