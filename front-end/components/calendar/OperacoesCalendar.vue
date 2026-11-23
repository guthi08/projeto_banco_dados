<script setup lang="ts">
import dayjs from 'dayjs'
import type { OperacaoVooRecord } from '~/stores/operacoes'

const props = defineProps<{
  operacoes: OperacaoVooRecord[]
}>()

const operacoesStore = useOperacoesStore()
const viewMode = ref<'month' | 'week'>('month')
const cursorDate = ref(dayjs().startOf('day'))

const weekDays = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']

const periodTitle = computed(() => {
  if (viewMode.value === 'month') {
    const raw = cursorDate.value.format('MMMM [de] YYYY')
    return raw.charAt(0).toUpperCase() + raw.slice(1)
  }
  const start = cursorDate.value.startOf('week')
  const end = start.add(6, 'day')
  return `${start.format('DD/MM')} — ${end.format('DD/MM/YYYY')}`
})

const calendarDays = computed(() => {
  const days: { iso: string; dayNum: number; inCurrentMonth: boolean; isToday: boolean }[] = []
  const todayIso = dayjs().format('YYYY-MM-DD')

  if (viewMode.value === 'week') {
    const start = cursorDate.value.startOf('week')
    for (let i = 0; i < 7; i += 1) {
      const d = start.add(i, 'day')
      days.push({
        iso: d.format('YYYY-MM-DD'),
        dayNum: d.date(),
        inCurrentMonth: true,
        isToday: d.format('YYYY-MM-DD') === todayIso
      })
    }
    return days
  }

  const firstOfMonth = cursorDate.value.startOf('month')
  const startGrid = firstOfMonth.startOf('week')
  for (let i = 0; i < 42; i += 1) {
    const d = startGrid.add(i, 'day')
    days.push({
      iso: d.format('YYYY-MM-DD'),
      dayNum: d.date(),
      inCurrentMonth: d.month() === cursorDate.value.month(),
      isToday: d.format('YYYY-MM-DD') === todayIso
    })
  }
  return days
})

const operationsByDay = computed(() => {
  const map = new Map<string, OperacaoVooRecord[]>()
  for (const op of props.operacoes) {
    const key = dayjs(op.dataHoraPlanejada).format('YYYY-MM-DD')
    const list = map.get(key) || []
    list.push(op)
    map.set(key, list)
  }
  for (const list of map.values()) {
    list.sort((a, b) => a.dataHoraPlanejada.localeCompare(b.dataHoraPlanejada))
  }
  return map
})

function hasSlotConflict(ops: OperacaoVooRecord[]): boolean {
  const active = ops.filter((o) => o.statusVoo === 'AGENDADO' || o.statusVoo === 'EM_ANDAMENTO')
  if (active.length < 2) return false
  for (let i = 0; i < active.length; i += 1) {
    for (let j = i + 1; j < active.length; j += 1) {
      const aStart = dayjs(active[i].dataHoraPlanejada)
      const aEnd = aStart.add(active[i].duracaoEstimadaMin || 60, 'minute')
      const bStart = dayjs(active[j].dataHoraPlanejada)
      const bEnd = bStart.add(active[j].duracaoEstimadaMin || 60, 'minute')
      const overlaps = aStart.isBefore(bEnd) && aEnd.isAfter(bStart)
      if (
        overlaps &&
        (active[i].droneCodigo === active[j].droneCodigo ||
          active[i].pilotoCpf === active[j].pilotoCpf)
      ) {
        return true
      }
    }
  }
  return false
}

function pillClass(status: OperacaoVooRecord['statusVoo']): string {
  switch (status) {
    case 'EM_ANDAMENTO':
      return 'border-amber-300 bg-amber-50 text-amber-900 hover:bg-amber-100'
    case 'AGENDADO':
      return 'border-blue-300 bg-blue-50 text-blue-900 hover:bg-blue-100'
    case 'CONCLUIDO':
      return 'border-emerald-300 bg-emerald-50 text-emerald-900 hover:bg-emerald-100'
    case 'CANCELADO':
      return 'border-rose-300 bg-rose-50 text-rose-800 line-through opacity-75 hover:bg-rose-100'
  }
}

function navigatePrev() {
  cursorDate.value =
    viewMode.value === 'month'
      ? cursorDate.value.subtract(1, 'month')
      : cursorDate.value.subtract(1, 'week')
}

function navigateNext() {
  cursorDate.value =
    viewMode.value === 'month'
      ? cursorDate.value.add(1, 'month')
      : cursorDate.value.add(1, 'week')
}

function goToday() {
  cursorDate.value = dayjs().startOf('day')
}
</script>

<template>
  <div class="p-4 sm:p-5">
    <!-- Toolbar do Calendário -->
    <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
      <div class="flex items-center gap-2">
        <UButton color="gray" variant="outline" size="xs" icon="i-lucide-chevron-left" aria-label="Período anterior" @click="navigatePrev" />
        <UButton color="gray" variant="outline" size="xs" @click="goToday">
          Hoje
        </UButton>
        <UButton color="gray" variant="outline" size="xs" icon="i-lucide-chevron-right" aria-label="Próximo período" @click="navigateNext" />
        <h3 class="ml-2 text-sm font-semibold text-slate-900 sm:text-base">
          {{ periodTitle }}
        </h3>
      </div>

      <div class="inline-flex rounded-lg border border-slate-200 bg-slate-50 p-0.5">
        <button
          type="button"
          class="rounded-md px-3 py-1 text-xs font-semibold transition-colors"
          :class="viewMode === 'month' ? 'bg-white text-blue-700 shadow-sm' : 'text-slate-600 hover:text-slate-900'"
          @click="viewMode = 'month'"
        >
          Mensal
        </button>
        <button
          type="button"
          class="rounded-md px-3 py-1 text-xs font-semibold transition-colors"
          :class="viewMode === 'week' ? 'bg-white text-blue-700 shadow-sm' : 'text-slate-600 hover:text-slate-900'"
          @click="viewMode = 'week'"
        >
          Semanal
        </button>
      </div>
    </div>

    <!-- Cabeçalho Dias da Semana -->
    <div class="grid grid-cols-7 border-x border-t border-slate-200 bg-slate-50">
      <div
        v-for="wd in weekDays"
        :key="wd"
        class="code-mono border-r border-slate-200 px-2 py-2 text-center text-[11px] font-semibold uppercase tracking-wider text-slate-500 last:border-r-0"
      >
        {{ wd }}
      </div>
    </div>

    <!-- Grid de Dias -->
    <div class="grid grid-cols-7 border border-slate-200">
      <div
        v-for="day in calendarDays"
        :key="day.iso"
        class="flex flex-col border-b border-r border-slate-200 p-2 last:border-r-0"
        :class="[
          viewMode === 'week' ? 'min-h-[220px]' : 'min-h-[112px]',
          day.inCurrentMonth ? 'bg-white' : 'bg-slate-50/70'
        ]"
      >
        <div class="mb-1.5 flex items-center justify-between">
          <span
            class="code-mono inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold"
            :class="
              day.isToday
                ? 'bg-blue-600 text-white'
                : day.inCurrentMonth
                  ? 'text-slate-800'
                  : 'text-slate-400'
            "
          >
            {{ day.dayNum }}
          </span>

          <div class="flex items-center gap-1">
            <span
              v-if="hasSlotConflict(operationsByDay.get(day.iso) || [])"
              class="inline-flex h-4 w-4 items-center justify-center rounded-full bg-red-100 text-red-700"
              title="Conflito de horário detectado"
            >
              <UIcon name="i-lucide-alert-triangle" class="h-3 w-3" />
            </span>

            <NuxtLink
              :to="`/operacoes/agendar?date=${day.iso}`"
              class="inline-flex h-5 w-5 items-center justify-center rounded text-slate-400 hover:bg-slate-100 hover:text-blue-600"
              title="Agendar missão neste dia"
            >
              <UIcon name="i-lucide-plus" class="h-3.5 w-3.5" />
            </NuxtLink>
          </div>
        </div>

        <div class="space-y-1">
          <NuxtLink
            v-for="op in operationsByDay.get(day.iso) || []"
            :key="op.codigo"
            :to="`/operacoes/${op.codigo}`"
            class="block rounded border px-1.5 py-1 text-left transition-colors"
            :class="pillClass(op.statusVoo)"
          >
            <div class="code-mono flex items-center justify-between gap-1 text-[10px] font-semibold">
              <span>{{ dayjs(op.dataHoraPlanejada).format('HH:mm') }}</span>
              <span>{{ op.droneCodigo }}</span>
            </div>
            <p class="truncate text-[11px] font-medium">
              {{ operacoesStore.resolveLabels(op).pilotName }}
            </p>
          </NuxtLink>
        </div>
      </div>
    </div>

    <!-- Legenda -->
    <div class="mt-3 flex flex-wrap items-center gap-4 text-xs text-slate-500">
      <span class="inline-flex items-center gap-1.5">
        <span class="h-2.5 w-2.5 rounded-sm border border-blue-300 bg-blue-100" />
        Agendado
      </span>
      <span class="inline-flex items-center gap-1.5">
        <span class="h-2.5 w-2.5 rounded-sm border border-amber-300 bg-amber-100" />
        Em Andamento
      </span>
      <span class="inline-flex items-center gap-1.5">
        <span class="h-2.5 w-2.5 rounded-sm border border-emerald-300 bg-emerald-100" />
        Concluído
      </span>
      <span class="inline-flex items-center gap-1.5">
        <span class="h-2.5 w-2.5 rounded-sm border border-rose-300 bg-rose-100" />
        Cancelado
      </span>
    </div>
  </div>
</template>
