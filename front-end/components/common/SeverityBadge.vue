<script setup lang="ts">
import type { NivelGravidade } from '~/types/models'

const props = defineProps<{
  severity: NivelGravidade
}>()

const severityMap: Record<NivelGravidade, { label: string; color: 'gray' | 'amber' | 'orange' | 'red' }> = {
  BAIXO: { label: 'Baixo', color: 'gray' },
  MEDIO: { label: 'Médio', color: 'amber' },
  ALTO: { label: 'Alto', color: 'orange' },
  CRITICO: { label: 'Crítico', color: 'red' }
}

const current = computed(() => severityMap[props.severity] || severityMap.BAIXO)
</script>

<template>
  <UBadge
    :color="current.color"
    variant="subtle"
    size="xs"
    class="code-mono rounded-full px-2.5 py-0.5 font-semibold uppercase"
  >
    {{ current.label }}
  </UBadge>
</template>
