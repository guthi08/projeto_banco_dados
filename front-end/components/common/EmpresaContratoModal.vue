<script setup lang="ts">
import type { EmpresaOperadoraRecord } from '~/stores/empresas'

const props = defineProps<{
  empresa: EmpresaOperadoraRecord | null
}>()

const emit = defineEmits<{
  close: []
  edit: [empresa: EmpresaOperadoraRecord]
  activate: [cnpj: string]
}>()

const empresasStore = useEmpresasStore()
const { formatDateBR } = useDateHelper()

const isOpen = computed({
  get: () => Boolean(props.empresa),
  set: (val: boolean) => {
    if (!val) emit('close')
  }
})

const isCurrentActive = computed(() => {
  if (!props.empresa || !empresasStore.activeOperator) return false
  return props.empresa.cnpj === empresasStore.activeOperator.cnpj
})

function statusBadgeColor(status: string): 'green' | 'amber' | 'gray' {
  if (status === 'Ativo') return 'green'
  if (status === 'Suspenso') return 'amber'
  return 'gray'
}
</script>

<template>
  <UModal v-model="isOpen" :ui="{ width: 'sm:max-w-lg' }">
    <UCard v-if="empresa">
      <template #header>
        <div class="flex items-start justify-between gap-3">
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <span class="code-mono text-xs font-semibold text-blue-600">
                {{ empresa.cnpj }}
              </span>
              <UBadge :color="statusBadgeColor(empresa.statusContrato)" variant="subtle" size="xs" class="code-mono">
                {{ empresa.statusContrato.toUpperCase() }}
              </UBadge>
              <UBadge v-if="isCurrentActive" color="blue" variant="solid" size="xs" class="code-mono">
                OPERADORA ATIVA
              </UBadge>
            </div>
            <h3 class="mt-1 text-base font-semibold text-slate-900">
              {{ empresa.razaoSocial }}
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
        <div class="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 sm:grid-cols-2">
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Setor de Atuação</span>
            <span class="text-sm font-semibold text-slate-900">{{ empresa.setorAtuacao }}</span>
          </div>
          <div>
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Formalização</span>
            <span class="code-mono text-sm font-semibold text-slate-900">{{ formatDateBR(empresa.dataContrato) }}</span>
          </div>
          <div class="sm:col-span-2">
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">E-mail Corporativo</span>
            <span class="text-sm text-slate-800">{{ empresa.email }}</span>
          </div>
          <div class="sm:col-span-2">
            <span class="code-mono block text-[10px] uppercase tracking-wider text-slate-500">Telefones Registrados</span>
            <div class="mt-1 flex flex-wrap gap-1.5">
              <span
                v-for="phone in empresa.telefones"
                :key="phone"
                class="code-mono inline-flex items-center gap-1 rounded-md border border-slate-200 bg-white px-2 py-0.5 text-xs text-slate-700"
              >
                <UIcon name="i-lucide-phone" class="h-3 w-3 text-slate-400" />
                {{ phone }}
              </span>
            </div>
          </div>
        </div>

        <!-- Recursos Subordinados -->
        <div class="grid grid-cols-3 gap-3">
          <div class="rounded-lg border border-slate-200 bg-white p-3 text-center">
            <span class="code-mono block text-xl font-bold text-slate-900">{{ empresa.dronesCount }}</span>
            <span class="text-xs text-slate-500">Drones</span>
          </div>
          <div class="rounded-lg border border-slate-200 bg-white p-3 text-center">
            <span class="code-mono block text-xl font-bold text-slate-900">{{ empresa.pilotosCount }}</span>
            <span class="text-xs text-slate-500">Pilotos</span>
          </div>
          <div class="rounded-lg border border-slate-200 bg-white p-3 text-center">
            <span class="code-mono block text-xl font-bold text-slate-900">{{ empresa.clientesCount }}</span>
            <span class="text-xs text-slate-500">Clientes</span>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex flex-wrap items-center justify-between gap-2">
          <UButton
            v-if="!isCurrentActive && empresa.statusContrato === 'Ativo'"
            color="blue"
            variant="soft"
            size="sm"
            icon="i-lucide-check-circle"
            @click="emit('activate', empresa.cnpj)"
          >
            Definir como Operadora Ativa
          </UButton>
          <span v-else class="text-xs text-slate-500">
            {{ isCurrentActive ? 'Operadora em contexto ativo no painel.' : 'Ative o contrato para selecionar.' }}
          </span>

          <div class="flex items-center gap-2">
            <UButton color="gray" variant="outline" size="sm" @click="emit('close')">
              Fechar
            </UButton>
            <UButton
              color="blue"
              size="sm"
              icon="i-lucide-pencil"
              @click="emit('edit', empresa)"
            >
              Editar
            </UButton>
          </div>
        </div>
      </template>
    </UCard>
  </UModal>
</template>
