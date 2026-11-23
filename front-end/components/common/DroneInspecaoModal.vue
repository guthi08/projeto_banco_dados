<script setup lang="ts">
import type { DroneRecord } from '~/stores/drones'

const props = defineProps<{
  drone: DroneRecord | null
}>()

const emit = defineEmits<{
  close: []
  edit: [drone: DroneRecord]
  renew: [codigo: string]
  toggleMaintenance: [codigo: string]
}>()

const empresasStore = useEmpresasStore()
const { formatDateBR, getValidityStatus, daysUntil } = useDateHelper()

const isOpen = computed({
  get: () => Boolean(props.drone),
  set: (val: boolean) => {
    if (!val) emit('close')
  }
})

const operatorName = computed(() => {
  if (!props.drone) return '—'
  const op = empresasStore.empresas.find((e) => e.cnpj === props.drone?.empresaOpeCnpj)
  return op?.razaoSocial || props.drone.empresaOpeCnpj
})

const validityBanner = computed(() => {
  if (!props.drone) return null
  const level = getValidityStatus(props.drone.dataValidadeCadastro, 30)
  const days = daysUntil(props.drone.dataValidadeCadastro)

  if (level === 'expired') {
    return {
      color: 'red' as const,
      icon: 'i-lucide-shield-x',
      title: 'Trava de Segurança ANAC Ativada',
      description: `Cadastro vencido há ${Math.abs(days)} dia(s). O sistema bloqueia qualquer agendamento ou início de voo com esta aeronave.`
    }
  }
  if (level === 'expiring') {
    return {
      color: 'amber' as const,
      icon: 'i-lucide-clock-alert',
      title: 'Renovação ANAC Recomendada',
      description: `Cadastro expira em ${days} dia(s) (${formatDateBR(props.drone.dataValidadeCadastro)}). Renove antes do prazo limite.`
    }
  }
  return {
    color: 'green' as const,
    icon: 'i-lucide-shield-check',
    title: 'Aeronave Apta para Despacho',
    description: `Documentação ANAC regular por mais ${days} dias (até ${formatDateBR(props.drone.dataValidadeCadastro)}).`
  }
})
</script>

<template>
  <UModal v-model="isOpen" :ui="{ width: 'sm:max-w-lg' }">
    <UCard v-if="drone">
      <template #header>
        <div class="flex items-start justify-between gap-3">
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <span class="code-mono text-sm font-bold text-blue-600">
                {{ drone.codigo }}
              </span>
              <CommonValidityBadge :valid-until="drone.dataValidadeCadastro" domain-label="ANAC" />
              <UBadge v-if="drone.emManutencao" color="gray" variant="solid" size="xs" class="code-mono">
                MANUTENÇÃO
              </UBadge>
            </div>
            <h3 class="mt-1 text-base font-semibold text-slate-900">
              {{ drone.fabricante }} {{ drone.modelo }}
            </h3>
          </div>

          <UButton
            color="gray"
            variant="ghost"
            icon="i-lucide-x"
            size="sm"
            aria-label="Fechar inspeção"
            @click="emit('close')"
          />
        </div>
      </template>

      <div class="space-y-4">
        <UAlert
          v-if="validityBanner"
          :color="validityBanner.color"
          variant="soft"
          :icon="validityBanner.icon"
          :title="validityBanner.title"
          :description="validityBanner.description"
        />

        <div class="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 sm:grid-cols-2">
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Nº de Série</span>
            <span class="code-mono text-sm font-semibold text-slate-900">{{ drone.numSerie }}</span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Peso Máx. Decolagem</span>
            <span class="code-mono text-sm font-semibold text-slate-900">{{ drone.pesoMaxDecolagem.toFixed(2) }} kg</span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Classe de Uso</span>
            <span class="text-sm font-semibold text-slate-900">
              {{ drone.tipoUso === 'avancado' ? 'Avançado' : 'Básico' }}
            </span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Validade ANAC</span>
            <span class="code-mono text-sm font-semibold text-slate-900">
              {{ formatDateBR(drone.dataValidadeCadastro) }}
            </span>
          </div>
          <div class="sm:col-span-2">
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Empresa Operadora</span>
            <span class="text-sm font-medium text-slate-900">{{ operatorName }}</span>
            <span class="code-mono ml-2 text-xs text-slate-500">({{ drone.empresaOpeCnpj }})</span>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex flex-wrap items-center justify-between gap-2">
          <div class="flex flex-wrap items-center gap-2">
            <UButton
              color="green"
              variant="soft"
              size="sm"
              icon="i-lucide-calendar-plus"
              @click="emit('renew', drone.codigo)"
            >
              Renovar ANAC (+12m)
            </UButton>
            <UButton
              color="gray"
              variant="soft"
              size="sm"
              icon="i-lucide-wrench"
              @click="emit('toggleMaintenance', drone.codigo)"
            >
              {{ drone.emManutencao ? 'Liberar Manutenção' : 'Colocar em Manutenção' }}
            </UButton>
          </div>

          <UButton
            color="blue"
            size="sm"
            icon="i-lucide-pencil"
            @click="emit('edit', drone)"
          >
            Editar
          </UButton>
        </div>
      </template>
    </UCard>
  </UModal>
</template>
