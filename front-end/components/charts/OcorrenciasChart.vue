<script setup lang="ts">
import { Bar } from 'vue-chartjs'
import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  LinearScale,
  Tooltip,
  type ChartData,
  type ChartOptions
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip)

const props = defineProps<{
  categories: { categoria: string; count: number }[]
}>()

const chartData = computed<ChartData<'bar'>>(() => ({
  labels: props.categories.map((c) => c.categoria),
  datasets: [
    {
      label: 'Ocorrências',
      data: props.categories.map((c) => c.count),
      backgroundColor: '#2563eb',
      borderRadius: 6,
      maxBarThickness: 24
    }
  ]
}))

const chartOptions: ChartOptions<'bar'> = {
  indexAxis: 'y',
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false
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
      beginAtZero: true,
      ticks: {
        stepSize: 1,
        color: '#64748b',
        font: {
          family: 'IBM Plex Mono, monospace',
          size: 11
        }
      },
      grid: {
        color: '#e2e8f0'
      }
    },
    y: {
      grid: {
        display: false
      },
      ticks: {
        color: '#334155',
        font: {
          family: 'IBM Plex Sans, sans-serif',
          size: 12
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
          Ocorrências Técnicas por Categoria
        </h3>
        <p class="text-xs text-slate-500">
          Incidência acumulada nas operações auditadas
        </p>
      </div>
      <span class="code-mono rounded-md bg-blue-50 px-2 py-0.5 text-xs font-semibold text-blue-700">
        Chart.js
      </span>
    </div>

    <div class="h-60 p-5">
      <ClientOnly>
        <Bar v-if="categories.length > 0" :data="chartData" :options="chartOptions" />
        <div v-else class="flex h-full items-center justify-center text-xs text-slate-400">
          Nenhuma ocorrência registrada para gerar gráfico.
        </div>
        <template #fallback>
          <div class="flex h-full items-center justify-center text-xs text-slate-400">
            Carregando gráfico...
          </div>
        </template>
      </ClientOnly>
    </div>
  </section>
</template>
