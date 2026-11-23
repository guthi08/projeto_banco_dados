<script setup lang="ts">
import dayjs from 'dayjs'
import type { FormSubmitEvent } from '#ui/types'
import {
  TIPOS_LOCAL,
  operacaoVooSchema,
  type OperacaoVooFormValues
} from '~/schemas/operacao.schema'

definePageMeta({
  layout: 'default',
  middleware: 'auth'
})

useHead({
  title: 'Agendar Operação de Voo — DRONE-MONITOR'
})

const route = useRoute()
const authStore = useAuthStore()
const operacoesStore = useOperacoesStore()
const empresasStore = useEmpresasStore()
const dronesStore = useDronesStore()
const pilotosStore = usePilotosStore()
const clientesStore = useClientesStore()
const toast = useToast()
const { daysUntil, formatDateBR } = useDateHelper()

const submitError = ref<string | null>(null)

const initialDate = computed(() => {
  const q = route.query.date
  if (typeof q === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(q)) {
    return `${q}T09:00`
  }
  return dayjs().add(1, 'day').hour(9).minute(0).format('YYYY-MM-DDTHH:mm')
})

const state = reactive<OperacaoVooFormValues>({
  empresaOpeCnpj: authStore.isAdmin
    ? (empresasStore.activeOperator?.cnpj || empresasStore.empresas[0]?.cnpj || authStore.user?.empresaCnpj || '')
    : (authStore.user?.empresaCnpj || empresasStore.activeOperator?.cnpj || ''),
  droneCodigo: '',
  pilotoCpf: '',
  clienteCpfRg: '',
  objetivoVoo: '',
  origem: '',
  destino: '',
  descricaoArea: '',
  tipoLocal: 'Rural',
  dataHoraPlanejada: initialDate.value,
  duracaoEstimadaMin: 60
})

const onlyDigits = (val: string) => val.replace(/\D/g, '')

watch(
  () => [empresasStore.empresas.length, authStore.user?.empresaCnpj] as const,
  () => {
    if (!state.empresaOpeCnpj) {
      state.empresaOpeCnpj = authStore.isAdmin
        ? (empresasStore.activeOperator?.cnpj || empresasStore.empresas[0]?.cnpj || authStore.user?.empresaCnpj || '')
        : (authStore.user?.empresaCnpj || '')
    }
  },
  { immediate: true }
)

watch(
  () => state.empresaOpeCnpj,
  (newCnpj, oldCnpj) => {
    if (oldCnpj && newCnpj !== oldCnpj) {
      state.droneCodigo = ''
      state.pilotoCpf = ''
      state.clienteCpfRg = ''
    }
  }
)

const flightDateIso = computed(() => {
  const parsed = dayjs(state.dataHoraPlanejada)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD')
})

const empresaOptions = computed(() =>
  empresasStore.empresas.map((e) => ({
    label: `${e.razaoSocial} (${e.statusContrato})`,
    value: e.cnpj,
    disabled: e.statusContrato !== 'Ativo'
  }))
)

const droneChoices = computed(() => {
  const selectedCnpj = onlyDigits(state.empresaOpeCnpj)
  return dronesStore.drones
    .filter((d) => !selectedCnpj || onlyDigits(d.empresaOpeCnpj) === selectedCnpj)
    .map((d) => {
      const remaining = daysUntil(d.dataValidadeCadastro, flightDateIso.value)
      const expired = remaining < 0
      const expiring = remaining >= 0 && remaining <= 30
      const blocked = expired || Boolean(d.emManutencao)
      const statusTag = d.emManutencao
        ? '🔧 MANUTENÇÃO'
        : expired
          ? '🔴 ANAC VENCIDO'
          : expiring
            ? `🟡 Vence em ${remaining}d`
            : '🟢 ANAC Válido'

      return {
        ...d,
        blocked,
        expired,
        expiring,
        statusTag,
        label: `${d.codigo} — ${d.fabricante} ${d.modelo} (${statusTag})`
      }
    })
})

const pilotoChoices = computed(() => {
  const selectedCnpj = onlyDigits(state.empresaOpeCnpj)
  return pilotosStore.pilotos
    .filter((p) => !selectedCnpj || onlyDigits(p.empresaOpeCnpj) === selectedCnpj)
    .map((p) => {
      const remaining = daysUntil(p.validadeCma, flightDateIso.value)
      const expired = remaining < 0
      const expiring = remaining >= 0 && remaining <= 30
      const statusTag = expired
        ? '🔴 CMA VENCIDO'
        : expiring
          ? `🟡 CMA vence em ${remaining}d`
          : '🟢 CMA Válido'

      return {
        ...p,
        blocked: expired,
        expired,
        expiring,
        statusTag,
        label: `${p.nome} · ${p.numRegistroLicenca} (${statusTag})`
      }
    })
})

const clienteChoices = computed(() => {
  const selectedCnpj = onlyDigits(state.empresaOpeCnpj)
  return clientesStore.clientes
    .filter((c) => !selectedCnpj || onlyDigits(c.empresaOpeCnpj) === selectedCnpj)
    .map((c) => ({
      ...c,
      blocked: c.statusContrato !== 'Ativo',
      label: `${c.nome} (${c.statusContrato === 'Ativo' ? 'Contrato Ativo' : 'Contrato Inativo'})`
    }))
})

const preFlightValidation = computed(() => {
  if (!state.droneCodigo || !state.pilotoCpf || !state.clienteCpfRg || !state.dataHoraPlanejada) {
    return null
  }
  return operacoesStore.validatePreFlight({
    droneCodigo: state.droneCodigo,
    pilotoCpf: state.pilotoCpf,
    clienteCpfRg: state.clienteCpfRg,
    dataHoraPlanejada: state.dataHoraPlanejada,
    duracaoEstimadaMin: state.duracaoEstimadaMin
  })
})

const stepperSteps = computed(() => [
  {
    num: 1,
    title: 'Recursos Alocados',
    done: Boolean(state.empresaOpeCnpj && state.droneCodigo && state.pilotoCpf && state.clienteCpfRg)
  },
  {
    num: 2,
    title: 'Trajeto & Horário',
    done: Boolean(
      state.objetivoVoo.length >= 5 &&
        state.origem.length >= 3 &&
        state.destino.length >= 3 &&
        state.origem.toLowerCase() !== state.destino.toLowerCase()
    )
  },
  {
    num: 3,
    title: 'Checklist Pré-Voo',
    done: Boolean(preFlightValidation.value?.valid)
  }
])

async function onSubmit(event: FormSubmitEvent<OperacaoVooFormValues>) {
  submitError.value = null
  try {
    const created = await operacoesStore.createOperacao(event.data)
    toast.add({
      title: `Missão ${created.displayCode} agendada`,
      description: `Aeronave ${created.droneCodigo} alocada com sucesso após aprovação pré-voo.`,
      icon: 'i-lucide-check-circle-2',
      color: 'green'
    })
    await navigateTo(`/operacoes/${created.codigo}`)
  } catch (err) {
    submitError.value = err instanceof Error ? err.message : extractApiErrorMessage(err)
  }
}

onMounted(() => {
  empresasStore.fetchEmpresas()
  dronesStore.fetchDrones()
  pilotosStore.fetchPilotos()
  clientesStore.fetchClientes()
  operacoesStore.fetchOperacoes()
})
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-6">
    <!-- Cabeçalho -->
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div>
        <NuxtLink to="/operacoes" class="mb-2 inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900">
          <UIcon name="i-lucide-arrow-left" class="h-3.5 w-3.5" />
          <span>Voltar para Operações</span>
        </NuxtLink>
        <h1 class="text-xl font-semibold text-slate-900 sm:text-2xl">
          Agendar & Despachar Missão
        </h1>
        <p class="mt-1 text-sm text-slate-500">
          Validação automática de validade ANAC, CMA do piloto e prevenção de conflito de horários.
        </p>
      </div>
    </div>

    <!-- Stepper Visual (Matriz 4.3) -->
    <ol class="grid gap-3 sm:grid-cols-3">
      <li
        v-for="step in stepperSteps"
        :key="step.num"
        class="flex items-center gap-3 rounded-xl border bg-white px-4 py-3 shadow-sm"
        :class="step.done ? 'border-emerald-300 bg-emerald-50/40' : 'border-slate-200'"
      >
        <span
          class="code-mono inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold"
          :class="step.done ? 'bg-emerald-600 text-white' : 'bg-sidebar-bg text-white'"
        >
          <UIcon v-if="step.done" name="i-lucide-check" class="h-4 w-4" />
          <template v-else>{{ step.num }}</template>
        </span>
        <div>
          <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">
            Etapa {{ step.num }}
          </span>
          <span class="text-xs font-semibold text-slate-900">{{ step.title }}</span>
        </div>
      </li>
    </ol>

    <UAlert
      v-if="submitError"
      color="red"
      variant="soft"
      icon="i-lucide-shield-x"
      title="Agendamento bloqueado"
      :description="submitError"
    />

    <UForm :schema="operacaoVooSchema" :state="state" class="grid gap-6 lg:grid-cols-12" @submit="onSubmit">
      <!-- Coluna Esquerda: Formulário -->
      <div class="space-y-6 lg:col-span-8">
        <!-- Bloco 1: Recursos -->
        <section class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 class="text-base font-semibold text-slate-900">
            1. Operadora e Alocação de Recursos
          </h2>
          <p class="mb-4 text-xs text-slate-500">
            Cada operação exige estritamente 1 drone válido, 1 piloto com CMA vigente e 1 cliente ativo.
          </p>

          <div class="space-y-4">
            <UFormGroup
              v-if="authStore.isAdmin"
              label="Empresa operadora"
              name="empresaOpeCnpj"
              required
            >
              <USelect
                v-model="state.empresaOpeCnpj"
                :options="empresaOptions"
                option-attribute="label"
                value-attribute="value"
              />
            </UFormGroup>

            <UFormGroup label="Aeronave (Drone — Trava ANAC)" name="droneCodigo" required>
              <USelectMenu
                v-model="state.droneCodigo"
                :options="droneChoices"
                value-attribute="codigo"
                option-attribute="label"
                searchable
                searchable-placeholder="Buscar prefixo PR/PP/PS ou modelo..."
                placeholder="Selecione o drone da frota"
              >
                <template #option="{ option }">
                  <div class="flex w-full items-center justify-between gap-2" :class="option.blocked ? 'opacity-50' : ''">
                    <div class="min-w-0">
                      <span class="code-mono font-semibold">{{ option.codigo }}</span>
                      <span class="ml-1.5 text-xs text-slate-600">{{ option.fabricante }} {{ option.modelo }}</span>
                    </div>
                    <span class="code-mono shrink-0 text-[11px]">{{ option.statusTag }}</span>
                  </div>
                </template>
              </USelectMenu>
            </UFormGroup>

            <UFormGroup label="Piloto Responsável (Trava CMA)" name="pilotoCpf" required>
              <USelectMenu
                v-model="state.pilotoCpf"
                :options="pilotoChoices"
                value-attribute="cpf"
                option-attribute="label"
                searchable
                searchable-placeholder="Buscar piloto por nome ou licença..."
                placeholder="Selecione o piloto habilitado"
              >
                <template #option="{ option }">
                  <div class="flex w-full items-center justify-between gap-2" :class="option.blocked ? 'opacity-50' : ''">
                    <div class="min-w-0">
                      <span class="font-semibold">{{ option.nome }}</span>
                      <span class="code-mono ml-1.5 text-[11px] text-slate-500">{{ option.numRegistroLicenca }}</span>
                    </div>
                    <span class="code-mono shrink-0 text-[11px]">{{ option.statusTag }}</span>
                  </div>
                </template>
              </USelectMenu>
            </UFormGroup>

            <UFormGroup label="Cliente Solicitante" name="clienteCpfRg" required>
              <USelectMenu
                v-model="state.clienteCpfRg"
                :options="clienteChoices"
                value-attribute="cpfRg"
                option-attribute="label"
                searchable
                searchable-placeholder="Buscar cliente..."
                placeholder="Selecione o cliente contratante"
              />
            </UFormGroup>
          </div>
        </section>

        <!-- Bloco 2: Trajeto e Janela -->
        <section class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 class="text-base font-semibold text-slate-900">
            2. Trajeto Declarativo e Janela de Voo
          </h2>
          <p class="mb-4 text-xs text-slate-500">
            Origem e destino devem ser locais distintos; o sistema verifica conflitos no intervalo planejado.
          </p>

          <div class="space-y-4">
            <UFormGroup label="Objetivo da missão" name="objetivoVoo" required>
              <UInput
                v-model="state.objetivoVoo"
                placeholder="Ex: Inspeção termográfica de linha de transmissão"
                icon="i-lucide-target"
              />
            </UFormGroup>

            <div class="grid gap-4 sm:grid-cols-2">
              <UFormGroup label="Local de origem (decolagem)" name="origem" required>
                <UInput
                  v-model="state.origem"
                  placeholder="Ex: Hangar Norte — Base Regional"
                  icon="i-lucide-map-pin"
                />
              </UFormGroup>

              <UFormGroup label="Local de destino (pouso)" name="destino" required>
                <UInput
                  v-model="state.destino"
                  placeholder="Ex: Setor Sul — Fazenda Boa Vista"
                  icon="i-lucide-flag"
                />
              </UFormGroup>
            </div>

            <div class="grid gap-4 sm:grid-cols-2">
              <UFormGroup label="Descrição da área sobrevoada" name="descricaoArea" required>
                <UInput
                  v-model="state.descricaoArea"
                  placeholder="Ex: Talhão Norte — 180 hectares"
                />
              </UFormGroup>

              <UFormGroup label="Tipo de local" name="tipoLocal" required>
                <USelect v-model="state.tipoLocal" :options="[...TIPOS_LOCAL]" />
              </UFormGroup>
            </div>

            <div class="grid gap-4 sm:grid-cols-2">
              <UFormGroup label="Data e hora planejadas" name="dataHoraPlanejada" required>
                <UInput
                  v-model="state.dataHoraPlanejada"
                  type="datetime-local"
                  class="code-mono"
                />
              </UFormGroup>

              <UFormGroup label="Duração estimada (minutos)" name="duracaoEstimadaMin" required>
                <UInput
                  v-model="state.duracaoEstimadaMin"
                  type="number"
                  min="15"
                  max="480"
                  step="15"
                  class="code-mono"
                />
              </UFormGroup>
            </div>
          </div>
        </section>
      </div>

      <!-- Coluna Direita: Painel Live de Validação Pré-Voo -->
      <aside class="space-y-4 lg:col-span-4">
        <section class="sticky top-6 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between border-b border-slate-200 pb-3">
            <h3 class="text-sm font-semibold text-slate-900">
              Checklist Pré-Voo (ANAC / CMA)
            </h3>
            <span class="code-mono text-[11px] text-slate-500">
              {{ formatDateBR(flightDateIso) }}
            </span>
          </div>

          <div v-if="!preFlightValidation" class="py-6 text-center text-xs text-slate-500">
            Selecione drone, piloto, cliente e data do voo para executar a checagem aeronáutica em tempo real.
          </div>

          <div v-else class="mt-4 space-y-3">
            <UAlert
              v-if="preFlightValidation.valid"
              color="green"
              variant="soft"
              icon="i-lucide-shield-check"
              title="Despacho Liberado"
              description="Drone regular na ANAC, piloto com CMA válido e sem conflitos de horário."
            />

            <UAlert
              v-for="(err, idx) in preFlightValidation.errors"
              :key="`err-${idx}`"
              color="red"
              variant="soft"
              icon="i-lucide-shield-x"
              title="Bloqueio de Segurança"
              :description="err"
            />

            <UAlert
              v-for="(warn, idx) in preFlightValidation.warnings"
              :key="`warn-${idx}`"
              color="amber"
              variant="soft"
              icon="i-lucide-clock-alert"
              title="Aviso Preventivo"
              :description="warn"
            />
          </div>

          <div class="mt-5 space-y-2 border-t border-slate-200 pt-4">
            <UButton
              type="submit"
              color="blue"
              size="lg"
              block
              icon="i-lucide-send"
              :loading="operacoesStore.loading"
              :disabled="Boolean(preFlightValidation && !preFlightValidation.valid)"
            >
              Confirmar Agendamento
            </UButton>
            <UButton to="/operacoes" color="gray" variant="ghost" size="sm" block>
              Cancelar
            </UButton>
          </div>
        </section>
      </aside>
    </UForm>
  </div>
</template>
