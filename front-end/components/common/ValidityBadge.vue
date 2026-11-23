<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    validUntil?: string | null
    warningDays?: number
    domainLabel?: string
  }>(),
  {
    validUntil: null,
    warningDays: 30,
    domainLabel: 'ANAC'
  }
)

const { daysUntil, getValidityStatus } = useDateHelper()

const remainingDays = computed(() => daysUntil(props.validUntil))
const level = computed(() => getValidityStatus(props.validUntil, props.warningDays))

const badgeMeta = computed(() => {
  if (level.value === 'expired') {
    const overdue = Math.abs(remainingDays.value)
    return {
      color: 'red' as const,
      dotClass: 'bg-red-600',
      label: overdue > 0 ? `Vencido há ${overdue}d` : 'Vencido'
    }
  }
  if (level.value === 'expiring') {
    return {
      color: 'amber' as const,
      dotClass: 'bg-amber-500',
      label: remainingDays.value === 0 ? 'Vence hoje' : `Vence em ${remainingDays.value}d`
    }
  }
  return {
    color: 'green' as const,
    dotClass: 'bg-emerald-600',
    label: `${props.domainLabel} Válido`
  }
})
</script>

<template>
  <UBadge
    :color="badgeMeta.color"
    variant="subtle"
    size="xs"
    class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 font-semibold"
  >
    <span class="h-1.5 w-1.5 rounded-full" :class="badgeMeta.dotClass" aria-hidden="true" />
    <span>{{ badgeMeta.label }}</span>
  </UBadge>
</template>
