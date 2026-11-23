<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    value: string | number
    meta: string
    tone?: 'green' | 'blue' | 'amber' | 'red' | 'neutral'
    icon?: string
  }>(),
  {
    tone: 'blue',
    icon: undefined
  }
)

const dotClassMap: Record<string, string> = {
  green: 'bg-emerald-500',
  blue: 'bg-blue-600',
  amber: 'bg-amber-500',
  red: 'bg-red-500',
  neutral: 'bg-slate-400'
}

const metaClassMap: Record<string, string> = {
  green: 'text-emerald-700',
  blue: 'text-blue-600',
  amber: 'text-amber-700',
  red: 'text-red-700',
  neutral: 'text-slate-500'
}
</script>

<template>
  <article class="rounded-xl border border-slate-200 bg-white px-5 py-4 shadow-sm transition-shadow hover:shadow">
    <div class="flex items-center justify-between gap-2">
      <span class="text-[13px] font-medium text-slate-600">
        {{ label }}
      </span>
      <UIcon
        v-if="icon"
        :name="icon"
        class="h-4 w-4 shrink-0"
        :class="metaClassMap[tone]"
        aria-hidden="true"
      />
      <span
        v-else
        class="h-2.5 w-2.5 shrink-0 rounded-full"
        :class="dotClassMap[tone]"
        aria-hidden="true"
      />
    </div>

    <div class="mt-2.5 flex flex-wrap items-baseline justify-between gap-2">
      <span class="code-mono text-3xl font-semibold leading-none text-slate-900">
        {{ value }}
      </span>
      <span class="text-xs font-medium" :class="metaClassMap[tone]">
        {{ meta }}
      </span>
    </div>
  </article>
</template>
