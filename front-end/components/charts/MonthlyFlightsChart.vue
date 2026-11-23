<script setup lang="ts">
import { Bar } from 'vue-chartjs'
import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip,
  type ChartData,
  type ChartOptions
} from 'chart.js'
import type { MonthlyFlightSeries } from '~/composables/useDashboardData'

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend)

const props = defineProps<{
  series: MonthlyFlightSeries
}>()

const chartData = computed<ChartData<'bar'>>(() => ({
  labels: props.series.labels,
  datasets: [
    {
      label: 'Planejados',
      data: props.series.planned,
      backgroundColor: '#2563eb',
      borderRadius: 6,
      maxBarThickness: 22
    },
    {
      label: 'Concluídos',
      data: props.series.completed,
      backgroundColor: '#16a34a',
      borderRadius: 6,
      maxBarThickness: 22
    }
  ]
}))

const chartOptions: ChartOptions<'bar'> = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'top',
      align: 'end',
      labels: {
        usePointStyle: true,
        boxWidth: 8,
        boxHeight: 8,
        color: '#475569',
        font: {
          family: 'IBM Plex Sans, sans-serif',
          size: 12
        }
      }
    },
    tooltip: {
      backgroundColor: '#131c2e',
      titleColor: '#f8fafc',
      bodyColor: '#e2e8f0',
      padding: 10,
      cornerRadius: 8
    }
  },
  scales: {
    x: {
      grid: {
        display: false
      },
      ticks: {
        color: '#64748b',
        font: {
          family: 'IBM Plex Mono, monospace',
          size: 11
        }
      }
    },
    y: {
      beginAtZero: true,
      grid: {
        color: '#e2e8f0'
      },
      ticks: {
        stepSize: 5,
        color: '#64748b',
        font: {
          family: 'IBM Plex Mono, monospace',
          size: 11
        }
      }
    }
  }
}
</script>

<template>
  <section class="rounded-xl border border-slate-200 bg-white shadow-sm">
    <div class="flex items-center justify-between border-b border-slate-200 px-5 py-3.5">
      <div>
        <h3 class="text-sm font-semibold text-slate-900">
          Volume de Voos por Mês
        </h3>
        <p class="text-xs text-slate-500">
          Comparativo dos últimos 6 meses (Planejados × Concluídos)
        </p>
      </div>
      <span class="code-mono rounded-md bg-blue-50 px-2 py-0.5 text-xs font-semibold text-blue-700">
        6M
      </span>
    </div>

    <div class="h-56 p-5">
      <ClientOnly>
        <Bar :data="chartData" :options="chartOptions" />
        <template #fallback>
          <div class="flex h-full items-center justify-center rounded-lg border border-dashed border-slate-200 text-xs text-slate-400">
            Carregando gráfico de barras...
          </div>
        </template>
      </ClientOnly>
    </div>
  </section>
</template>
