import { defineStore } from 'pinia'
import type { StatusVoo } from '~/types/models'

export type PeriodFilter = '7d' | '30d' | '90d' | 'all'
export type StatusFilterValue = 'TODOS' | StatusVoo

export const useFiltersStore = defineStore('filters', () => {
  const period = ref<PeriodFilter>('30d')
  const operationStatus = ref<StatusFilterValue>('TODOS')
  const globalSearch = ref('')

  function resetFilters() {
    period.value = '30d'
    operationStatus.value = 'TODOS'
    globalSearch.value = ''
  }

  return {
    period,
    operationStatus,
    globalSearch,
    resetFilters
  }
})
