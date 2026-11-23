<script setup lang="ts">
import type { StatusVoo } from '~/types/models'

definePageMeta({
  layout: 'default',
  middleware: 'auth'
})

const route = useRoute()
const operacoesStore = useOperacoesStore()
const auditoriasStore = useAuditoriasStore()
const empresasStore = useEmpresasStore()
const toast = useToast()
const { formatDateTimeBR } = useDateHelper()

const codigo = computed(() => Number(route.params.id))
const operacao = computed(() => operacoesStore.getByCodigo(codigo.value))
const resolved = computed(() => (operacao.value ? operacoesStore.resolveLabels(operacao.value) : null))
const flightAudits = computed(() => auditoriasStore.getByOperation(codigo.value))

const operatorName = computed(() => {
  if (!operacao.value) return '—'
  return (
    empresasStore.empresas.find((e) => e.cnpj === operacao.value?.empresaOpeCnpj)?.razaoSocial ||
    operacao.value.empresaOpeCnpj
  )
})

const auditModalOpen = ref(false)

useHead({
  title: computed(() =>
    operacao.value ? `${operacao.value.displayCode} — Operação de Voo` : 'Operação não encontrada'
  )
})

async function transitionTo(nextStatus: StatusVoo) {
  if (!operacao.value) return
  try {
    const updated = await operacoesStore.updateStatus(operacao.value.codigo, nextStatus)
    toast.add({
      title: `Status atualizado: ${updated.displayCode}`,
      description: `Operação marcada como ${nextStatus.replace('_', ' ')}.`,
      icon: 'i-lucide-navigation',
      color: nextStatus === 'CANCELADO' ? 'red' : 'green'
    })
  } catch (err) {
    toast.add({
      title: 'Ação bloqueada',
      description: err instanceof Error ? err.message : extractApiErrorMessage(err),
      icon: 'i-lucide-shield-alert',
      color: 'red'
    })
  }
}

onMounted(() => {
  operacoesStore.fetchOperacoes()
  auditoriasStore.fetchAuditorias()
})
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-6">
    <div>
      <NuxtLink to="/operacoes" class="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900">
        <UIcon name="i-lucide-arrow-left" class="h-3.5 w-3.5" />
        <span>Voltar para Operações de Voo</span>
      </NuxtLink>
    </div>

    <div v-if="!operacao" class="rounded-xl border border-slate-200 bg-white p-10 text-center shadow-sm">
      <UIcon name="i-lucide-search-x" class="mx-auto h-10 w-10 text-slate-400" />
      <h1 class="mt-3 text-lg font-semibold text-slate-900">Operação não encontrada</h1>
      <p class="mt-1 text-sm text-slate-500">
        O código informado não corresponde a nenhuma missão registrada.
      </p>
      <UButton to="/operacoes" color="blue" class="mt-4">
        Ver todas as operações
      </UButton>
    </div>

    <template v-else>
      <!-- Cabeçalho da Missão -->
      <div class="flex flex-wrap items-start justify-between gap-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <div>
          <div class="flex flex-wrap items-center gap-2.5">
            <span class="code-mono rounded-md bg-blue-50 px-2.5 py-1 text-xs font-bold text-blue-700">
              {{ operacao.displayCode }}
            </span>
            <CommonStatusBadge :status="operacao.statusVoo" />
            <UBadge color="gray" variant="subtle" size="xs">{{ operacao.tipoLocal }}</UBadge>
          </div>
          <h1 class="mt-2 text-xl font-semibold text-slate-900">
            {{ operacao.objetivoVoo }}
          </h1>
          <p class="mt-1 text-xs text-slate-500">
            Operadora: <strong class="text-slate-800">{{ operatorName }}</strong>
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <UButton
            v-if="operacao.statusVoo === 'AGENDADO'"
            color="green"
            icon="i-lucide-play"
            @click="transitionTo('EM_ANDAMENTO')"
          >
            Iniciar Voo
          </UButton>
          <UButton
            v-if="operacao.statusVoo === 'EM_ANDAMENTO'"
            color="blue"
            icon="i-lucide-check-circle-2"
            @click="transitionTo('CONCLUIDO')"
          >
            Registrar Pouso (Concluir)
          </UButton>
          <UButton
            v-if="operacao.statusVoo === 'AGENDADO' || operacao.statusVoo === 'EM_ANDAMENTO'"
            color="red"
            variant="soft"
            icon="i-lucide-x-circle"
            @click="transitionTo('CANCELADO')"
          >
            Cancelar
          </UButton>
          <UButton
            color="amber"
            variant="soft"
            icon="i-lucide-shield-plus"
            @click="auditModalOpen = true"
          >
            Lançar Ocorrência
          </UButton>
        </div>
      </div>

      <!-- Detalhes da Rota e Horários -->
      <div class="grid gap-6 lg:grid-cols-12">
        <section class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm lg:col-span-7">
          <h2 class="text-base font-semibold text-slate-900">
            Trajeto e Cronograma Operacional
          </h2>

          <div class="mt-4 grid gap-4 rounded-lg border border-slate-200 bg-slate-50 p-4 sm:grid-cols-2">
            <div>
              <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Origem (Decolagem)</span>
              <span class="text-sm font-semibold text-slate-900">{{ operacao.origem }}</span>
            </div>
            <div>
              <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Destino (Pouso)</span>
              <span class="text-sm font-semibold text-slate-900">{{ operacao.destino }}</span>
            </div>
            <div class="sm:col-span-2">
              <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Área Sobrevoada</span>
              <span class="text-sm text-slate-800">{{ operacao.descricaoArea }}</span>
            </div>
            <div>
              <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Decolagem Planejada</span>
              <span class="code-mono text-sm font-semibold text-slate-900">
                {{ formatDateTimeBR(operacao.dataHoraPlanejada) }}
              </span>
            </div>
            <div>
              <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Duração Estimada</span>
              <span class="code-mono text-sm font-semibold text-slate-900">
                {{ operacao.duracaoEstimadaMin }} minutos
              </span>
            </div>
            <div>
              <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Decolagem Real</span>
              <span class="code-mono text-sm font-semibold text-slate-900">
                {{ formatDateTimeBR(operacao.dataHoraReal) }}
              </span>
            </div>
            <div>
              <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Pouso Real</span>
              <span class="code-mono text-sm font-semibold text-slate-900">
                {{ formatDateTimeBR(operacao.dataHoraPousoReal) }}
              </span>
            </div>
          </div>
        </section>

        <!-- Recursos Alocados -->
        <section class="space-y-3 rounded-xl border border-slate-200 bg-white p-6 shadow-sm lg:col-span-5">
          <h2 class="text-base font-semibold text-slate-900">
            Recursos Alocados
          </h2>

          <div class="rounded-lg border border-slate-200 bg-slate-50 p-3.5">
            <div class="flex items-center justify-between gap-2">
              <span class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">Aeronave</span>
              <CommonValidityBadge
                v-if="resolved?.drone"
                :valid-until="resolved.drone.dataValidadeCadastro"
                domain-label="ANAC"
              />
            </div>
            <p class="mt-1 text-sm font-semibold text-slate-900">
              <span class="code-mono text-blue-700">{{ operacao.droneCodigo }}</span> · {{ resolved?.droneModel }}
            </p>
          </div>

          <div class="rounded-lg border border-slate-200 bg-slate-50 p-3.5">
            <div class="flex items-center justify-between gap-2">
              <span class="code-mono text-[10px] font-semibold uppercase tracking-wider text-slate-500">Piloto Responsável</span>
              <CommonValidityBadge
                v-if="resolved?.piloto"
                :valid-until="resolved.piloto.validadeCma"
                domain-label="CMA"
              />
            </div>
            <p class="mt-1 text-sm font-semibold text-slate-900">{{ resolved?.pilotName }}</p>
            <span class="code-mono text-xs text-slate-500">{{ operacao.pilotoCpf }}</span>
          </div>

          <div class="rounded-lg border border-slate-200 bg-slate-50 p-3.5">
            <span class="code-mono block text-[10px] font-semibold uppercase tracking-wider text-slate-500">Cliente Solicitante</span>
            <p class="mt-1 text-sm font-semibold text-slate-900">{{ resolved?.clientName }}</p>
            <span class="code-mono text-xs text-slate-500">{{ operacao.clienteCpfRg }}</span>
          </div>
        </section>
      </div>

      <!-- Auditorias & Ocorrências desta Missão -->
      <section class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-6 py-4">
          <div>
            <h2 class="text-base font-semibold text-slate-900">
              Auditoria e Ocorrências desta Operação
            </h2>
            <p class="text-xs text-slate-500">
              Registros técnicos imutáveis vinculados exclusivamente ao voo {{ operacao.displayCode }}.
            </p>
          </div>
          <UButton color="blue" size="sm" icon="i-lucide-plus" @click="auditModalOpen = true">
            Nova Ocorrência
          </UButton>
        </div>

        <div class="p-6">
          <div
            v-if="flightAudits.length === 0"
            class="rounded-lg border border-dashed border-slate-200 p-6 text-center text-sm text-slate-500"
          >
            Nenhuma ocorrência técnica registrada para esta missão.
          </div>

          <div v-else class="space-y-3">
            <article
              v-for="audit in flightAudits"
              :key="audit.displayId"
              class="rounded-lg border border-slate-200 bg-slate-50 p-4"
            >
              <div class="flex flex-wrap items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <span class="code-mono text-xs font-bold text-slate-800">{{ audit.displayId }}</span>
                  <span class="text-sm font-semibold text-slate-900">{{ audit.categoria }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="code-mono text-xs text-slate-500">{{ formatDateTimeBR(audit.dataHoraEvento) }}</span>
                  <CommonSeverityBadge :severity="audit.nivelGravidade" />
                </div>
              </div>
              <p class="mt-2 text-sm leading-relaxed text-slate-700">
                {{ audit.descricao }}
              </p>
            </article>
          </div>
        </div>
      </section>

      <FormsAuditoriaFormModal
        v-model="auditModalOpen"
        :locked-operacao-codigo="operacao.codigo"
      />
    </template>
  </div>
</template>
