<script setup lang="ts">
import type { StatusVoo } from '~/types/models'

const props = defineProps<{
  status: StatusVoo
}>()

const configMap: Record<StatusVoo, { label: string; color: 'blue' | 'amber' | 'green' | 'red'; pulse?: boolean; dotClass: string }> = {
  AGENDADO: {
    label: 'Agendado',
    color: 'blue',
    dotClass: 'bg-blue-600'
  },
  EM_ANDAMENTO: {
    label: 'Em Andamento',
    color: 'amber',
    pulse: true,
    dotClass: 'bg-amber-600'
  },
  CONCLUIDO: {
    label: 'Concluído',
    color: 'green',
    dotClass: 'bg-emerald-600'
  },
  CANCELADO: {
    label: 'Cancelado',
    color: 'red',
    dotClass: 'bg-rose-600'
  }
}

const current = computed(() => configMap[props.status] || configMap.AGENDADO)
</script>

<template>
  <UBadge
    :color="current.color"
    variant="subtle"
    size="xs"
    :class="['inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 font-semibold', current.pulse ? 'animate-pulse' : '']"
  >
    <span class="h-1.5 w-1.5 rounded-full" :class="current.dotClass" aria-hidden="true" />
    <span>{{ current.label }}</span>
  </UBadge>
</template>
