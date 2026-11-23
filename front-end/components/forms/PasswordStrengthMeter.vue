<script setup lang="ts">
import { evaluatePasswordStrength } from '~/schemas/auth.schema'

const props = defineProps<{
  password: string
}>()

const strength = computed(() => evaluatePasswordStrength(props.password))
</script>

<template>
  <div class="mt-2 rounded-lg border border-slate-200 bg-slate-50 p-3">
    <div class="mb-2 flex items-center justify-between gap-2">
      <span class="code-mono text-[11px] font-medium uppercase tracking-wider text-slate-500">
        Força da senha
      </span>
      <span class="code-mono text-xs font-semibold" :class="strength.textClass">
        {{ strength.label }}
      </span>
    </div>

    <div class="grid grid-cols-4 gap-1.5" role="progressbar" :aria-valuenow="strength.score" aria-valuemin="0" aria-valuemax="4">
      <div
        v-for="bar in 4"
        :key="bar"
        class="h-1.5 rounded-full transition-colors duration-200"
        :class="bar <= strength.score ? strength.barClass : 'bg-slate-200'"
      />
    </div>

    <ul class="mt-2.5 grid grid-cols-2 gap-x-3 gap-y-1">
      <li
        v-for="item in strength.criteria"
        :key="item.key"
        class="flex items-center gap-1.5 text-xs transition-colors"
        :class="item.met ? 'text-emerald-700 font-medium' : 'text-slate-500'"
      >
        <UIcon
          :name="item.met ? 'i-lucide-check-circle-2' : 'i-lucide-circle'"
          class="h-3.5 w-3.5 shrink-0"
        />
        <span>{{ item.label }}</span>
      </li>
    </ul>
  </div>
</template>
