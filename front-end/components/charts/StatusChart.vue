<script setup lang="ts">
import { Doughnut } from 'vue-chartjs'
import {
  ArcElement,
  Chart as ChartJS,
  Legend,
  Tooltip,
  type ChartData,
  type ChartOptions
} from 'chart.js'
import type { StatusVoo } from '~/types/models'

ChartJS.register(ArcElement, Tooltip, Legend)

const props = defineProps<{
  counts: Record<StatusVoo, number>
}>()

const total = computed(() =>
  Object.values(props.counts).reduce((acc, current) => acc + current, 0)
)

const legendItems = computed(() => [
  { key: 'AGENDADO', label: 'Agendado', count: props.counts.AGENDADO, dotClass: 'bg-blue-600' },
  { key: 'EM_ANDAMENTO', label: 'Em Andamento', count: props.counts.EM_ANDAMENTO, dotClass: 'bg-amber-500' },
  { key: 'CONCLUIDO', label: 'Concluído', count: props.counts.CONCLUIDO, dotClass: 'bg-emerald-600' },
  { key: 'CANCELADO', label: 'Cancelado', count: props.counts.CANCELADO, dotClass: 'bg-rose-600' }
])

const chartData = computed<ChartData<'doughnut'>>(() => ({
  labels: ['Agendado', 'Em Andamento', 'Concluído', 'Cancelado'],
  datasets: [
    {
      data: [
        props.counts.AGENDADO,
        props.counts.EM_ANDAMENTO,
        props.counts.CONCLUIDO,
        props.counts.CANCELADO
      ],
      backgroundColor: ['#2563eb', '#f59e0b', '#16a34a', '#e11d48'],
      borderColor: '#ffffff',
      borderWidth: 2,
      hoverOffset: 5
    }
  ]
}))

const chartOptions: ChartOptions<'doughnut'> = {
  responsive: true,
  maintainAspectRatio: false,
  cutout: '68%',
  plugins: {
    legend: {
      display: false
    },
    tooltip: {
      backgroundColor: '#131c2e',
      titleColor: '#f8fafc',
      bodyColor: '#e2e8f0',
      padding: 10,
      cornerRadius: 8,
      displayColors: true
    }
  }
}
</script>

<template>
  <section class="rounded-xl border border-slate-200 bg-white shadow-sm">
    <div class="flex items-center justify-between border-b border-slate-200 px-5 py-3.5">
      <div>
        <h3 class="text-sm font-semibold text-slate-900">
          Distribuição por Status de Voo
        </h3>
        <p class="text-xs text-slate-500">
          Proporção operacional no período selecionado
        </p>
      </div>
      <span class="code-mono rounded-md bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-700">
        {{ total }} voos
      </span>
    </div>

    <div class="grid items-center gap-5 p-5 sm:grid-cols-12">
      <div class="relative mx-auto h-44 w-44 sm:col-span-5">
        <ClientOnly>
          <Doughnut :data="chartData" :options="chartOptions" />
          <template #fallback>
            <div class="flex h-full w-full items-center justify-center rounded-full border-4 border-slate-100 text-xs text-slate-400">
              Carregando gráfico...
            </div>
          </template>
        </ClientOnly>
        <div class="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <span class="code-mono text-2xl font-bold text-slate-900">{{ total }}</span>
          <span class="code-mono text-[10px] uppercase tracking-wider text-slate-500">Total</span>
        </div>
      </div>

      <ul class="space-y-2.5 sm:col-span-7">
        <li
          v-for="item in legendItems"
          :key="item.key"
          class="flex items-center justify-between rounded-lg border border-slate-100 bg-slate-50 px-3 py-2"
        >
          <div class="flex items-center gap-2.5">
            <span class="h-2.5 w-2.5 rounded-full" :class="item.dotClass" />
            <span class="text-xs font-medium text-slate-700">{{ item.label }}</span>
          </div>
          <div class="code-mono flex items-center gap-2 text-xs">
            <span class="font-semibold text-slate-900">{{ item.count }}</span>
            <span class="text-slate-400">
              ({{ total > 0 ? Math.round((item.count / total) * 100) : 0 }}%)
            </span>
          </div>
        </li>
      </ul>
    </div>
  </section>
</template>
