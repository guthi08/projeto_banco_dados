<script setup lang="ts">
withDefaults(
  defineProps<{
    theme?: 'dark' | 'light'
    compact?: boolean
  }>(),
  {
    theme: 'dark',
    compact: false
  }
)
</script>

<template>
  <div class="inline-flex items-center gap-2.5 select-none">
    <span
      class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-blue-600 text-white shadow-sm"
      aria-hidden="true"
    >
      <svg
        width="20"
        height="20"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2.1"
        stroke-linecap="round"
        stroke-linejoin="round"
        xmlns="http://www.w3.org/2000/svg"
      >
        <circle cx="12" cy="12" r="2.5" />
        <path d="M5 5l4.5 4.5M19 5l-4.5 4.5M5 19l4.5-4.5M19 19l-4.5-4.5" />
        <circle cx="4.5" cy="4.5" r="2.2" />
        <circle cx="19.5" cy="4.5" r="2.2" />
        <circle cx="4.5" cy="19.5" r="2.2" />
        <circle cx="19.5" cy="19.5" r="2.2" />
      </svg>
    </span>

    <div v-if="!compact" class="leading-tight">
      <span
        class="block text-base font-bold tracking-[0.06em]"
        :class="theme === 'dark' ? 'text-slate-50' : 'text-slate-900'"
      >
        DRONE-MONITOR
      </span>
    </div>
  </div>
</template>
