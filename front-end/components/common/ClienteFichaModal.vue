<script setup lang="ts">
import type { ClienteRecord } from '~/stores/clientes'

const props = defineProps<{
  cliente: ClienteRecord | null
}>()

const emit = defineEmits<{
  close: []
  edit: [cliente: ClienteRecord]
}>()

const empresasStore = useEmpresasStore()
const { operations } = useDashboardData()
const { formatDateBR, formatDateTimeBR } = useDateHelper()

const isOpen = computed({
  get: () => Boolean(props.cliente),
  set: (val: boolean) => {
    if (!val) emit('close')
  }
})

const operatorName = computed(() => {
  if (!props.cliente) return '—'
  return (
    empresasStore.empresas.find((e) => e.cnpj === props.cliente?.empresaOpeCnpj)?.razaoSocial ||
    props.cliente.empresaOpeCnpj
  )
})

const clientOperations = computed(() => {
  if (!props.cliente) return []
  const targetName = props.cliente.nome.toLowerCase()
  return operations.value.filter((op) => op.clientName.toLowerCase() === targetName)
})
</script>

<template>
  <UModal v-model="isOpen" :ui="{ width: 'sm:max-w-2xl' }">
    <UCard v-if="cliente">
      <template #header>
        <div class="flex items-start justify-between gap-3">
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <span class="code-mono text-xs font-semibold text-blue-600">
                {{ cliente.cpfRg }}
              </span>
              <UBadge
                :color="cliente.statusContrato === 'Ativo' ? 'green' : 'gray'"
                variant="subtle"
                size="xs"
                class="code-mono"
              >
                CONTRATO {{ cliente.statusContrato.toUpperCase() }}
              </UBadge>
            </div>
            <h3 class="mt-1 text-base font-semibold text-slate-900">
              {{ cliente.nome }}
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

      <div class="space-y-5">
        <!-- Dados Cadastrais e Contrato -->
        <div class="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 sm:grid-cols-2">
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">RG / Inscrição</span>
            <span class="code-mono text-sm font-semibold text-slate-900">{{ cliente.rg }}</span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Data de Formalização</span>
            <span class="code-mono text-sm font-semibold text-slate-900">{{ formatDateBR(cliente.dataFormalizacao) }}</span>
          </div>
          <div class="sm:col-span-2">
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Empresa Operadora Vinculada</span>
            <span class="text-sm font-semibold text-slate-900">{{ operatorName }}</span>
            <span class="code-mono ml-2 text-xs text-slate-500">({{ cliente.empresaOpeCnpj }})</span>
          </div>
          <div v-if="cliente.demandaPrincipal" class="sm:col-span-2">
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Demanda / Serviço Contratado</span>
            <span class="text-sm text-slate-800">{{ cliente.demandaPrincipal }}</span>
          </div>
          <div class="sm:col-span-2">
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Telefones de Contato</span>
            <div class="mt-1 flex flex-wrap gap-1.5">
              <span
                v-for="phone in cliente.telefones"
                :key="phone"
                class="code-mono inline-flex items-center gap-1 rounded-md border border-slate-200 bg-white px-2 py-0.5 text-xs text-slate-700"
              >
                <UIcon name="i-lucide-phone" class="h-3 w-3 text-slate-400" />
                {{ phone }}
              </span>
            </div>
          </div>
        </div>

        <!-- Histórico de Voos por Tomador -->
        <div>
          <div class="mb-2.5 flex items-center justify-between">
            <h4 class="text-sm font-semibold text-slate-900">
              Histórico de Operações de Voo
            </h4>
            <span class="code-mono rounded bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-700">
              {{ clientOperations.length }} registro(s)
            </span>
          </div>

          <div
            v-if="clientOperations.length === 0"
            class="rounded-lg border border-dashed border-slate-200 p-5 text-center text-xs text-slate-500"
          >
            Nenhuma operação de voo registrada para este cliente até o momento.
          </div>

          <div v-else class="space-y-2">
            <div
              v-for="op in clientOperations"
              :key="op.code"
              class="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-slate-200 bg-white px-3.5 py-2.5"
            >
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <span class="code-mono text-xs font-semibold text-slate-900">{{ op.code }}</span>
                  <span class="code-mono text-xs text-slate-500">{{ op.droneCode }}</span>
                </div>
                <p class="truncate text-xs text-slate-700">{{ op.objective }}</p>
                <span class="code-mono text-[11px] text-slate-400">
                  Piloto: {{ op.pilotName }} · {{ formatDateTimeBR(op.plannedAt) }}
                </span>
              </div>
              <CommonStatusBadge :status="op.status" />
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-2">
          <UButton color="gray" variant="outline" size="sm" @click="emit('close')">
            Fechar
          </UButton>
          <UButton color="blue" size="sm" icon="i-lucide-pencil" @click="emit('edit', cliente)">
            Editar Cliente
          </UButton>
        </div>
      </template>
    </UCard>
  </UModal>
</template>
