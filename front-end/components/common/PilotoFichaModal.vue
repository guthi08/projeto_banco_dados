<script setup lang="ts">
import type { PilotoRecord } from '~/stores/pilotos'

const props = defineProps<{
  piloto: PilotoRecord | null
}>()

const emit = defineEmits<{
  close: []
  edit: [piloto: PilotoRecord]
  renewCma: [cpf: string]
}>()

const empresasStore = useEmpresasStore()
const { formatDateBR, getValidityStatus, daysUntil } = useDateHelper()

const isOpen = computed({
  get: () => Boolean(props.piloto),
  set: (val: boolean) => {
    if (!val) emit('close')
  }
})

const operatorName = computed(() => {
  if (!props.piloto) return '—'
  return empresasStore.empresas.find((e) => e.cnpj === props.piloto?.empresaOpeCnpj)?.razaoSocial || props.piloto.empresaOpeCnpj
})

const cmaBanner = computed(() => {
  if (!props.piloto) return null
  const level = getValidityStatus(props.piloto.validadeCma, 30)
  const days = daysUntil(props.piloto.validadeCma)

  if (level === 'expired') {
    return {
      color: 'red' as const,
      icon: 'i-lucide-shield-x',
      title: 'Trava de Segurança CMA Ativada',
      description: `Certificado Médico Aeronáutico vencido há ${Math.abs(days)} dia(s). Piloto bloqueado para novas escalas até renovação.`
    }
  }
  if (level === 'expiring') {
    return {
      color: 'amber' as const,
      icon: 'i-lucide-clock-alert',
      title: 'CMA Próximo do Vencimento',
      description: `CMA vence em ${days} dia(s) (${formatDateBR(props.piloto.validadeCma)}). Solicite exame médico periódico.`
    }
  }
  return {
    color: 'green' as const,
    icon: 'i-lucide-shield-check',
    title: 'Piloto Apto para Escalas',
    description: `CMA regular por mais ${days} dias (válido até ${formatDateBR(props.piloto.validadeCma)}).`
  }
})
</script>

<template>
  <UModal v-model="isOpen" :ui="{ width: 'sm:max-w-lg' }">
    <UCard v-if="piloto">
      <template #header>
        <div class="flex items-start justify-between gap-3">
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <span class="code-mono text-xs font-bold text-blue-600">
                {{ piloto.numRegistroLicenca }}
              </span>
              <CommonValidityBadge :valid-until="piloto.validadeCma" domain-label="CMA" />
            </div>
            <h3 class="mt-1 text-base font-semibold text-slate-900">
              {{ piloto.nome }}
            </h3>
          </div>

          <UButton
            color="gray"
            variant="ghost"
            icon="i-lucide-x"
            size="sm"
            aria-label="Fechar ficha"
            @click="emit('close')"
          />
        </div>
      </template>

      <div class="space-y-4">
        <UAlert
          v-if="cmaBanner"
          :color="cmaBanner.color"
          variant="soft"
          :icon="cmaBanner.icon"
          :title="cmaBanner.title"
          :description="cmaBanner.description"
        />

        <div class="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 sm:grid-cols-2">
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">CPF</span>
            <span class="code-mono text-sm font-semibold text-slate-900">{{ piloto.cpf }}</span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">RG</span>
            <span class="code-mono text-sm font-semibold text-slate-900">{{ piloto.rg }}</span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Validade CMA</span>
            <span class="code-mono text-sm font-semibold text-slate-900">{{ formatDateBR(piloto.validadeCma) }}</span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Voos Registrados</span>
            <span class="code-mono text-sm font-semibold text-slate-900">{{ piloto.voosRealizados ?? 0 }}</span>
          </div>
          <div class="sm:col-span-2">
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">E-mail</span>
            <span class="text-sm text-slate-800">{{ piloto.email }}</span>
          </div>
          <div class="sm:col-span-2">
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Empresa Operadora</span>
            <span class="text-sm font-medium text-slate-900">{{ operatorName }}</span>
          </div>
          <div class="sm:col-span-2">
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Telefones</span>
            <div class="mt-1 flex flex-wrap gap-1.5">
              <span
                v-for="phone in piloto.telefones"
                :key="phone"
                class="code-mono inline-flex items-center gap-1 rounded-md border border-slate-200 bg-white px-2 py-0.5 text-xs text-slate-700"
              >
                <UIcon name="i-lucide-phone" class="h-3 w-3 text-slate-400" />
                {{ phone }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex flex-wrap items-center justify-between gap-2">
          <UButton
            color="green"
            variant="soft"
            size="sm"
            icon="i-lucide-calendar-plus"
            @click="emit('renewCma', piloto.cpf)"
          >
            Renovar CMA (+12m)
          </UButton>

          <div class="flex items-center gap-2">
            <UButton color="gray" variant="outline" size="sm" @click="emit('close')">
              Fechar
            </UButton>
            <UButton color="blue" size="sm" icon="i-lucide-pencil" @click="emit('edit', piloto)">
              Editar
            </UButton>
          </div>
        </div>
      </template>
    </UCard>
  </UModal>
</template>
