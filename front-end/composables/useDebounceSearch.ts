import { refDebounced } from '@vueuse/core'

export function useDebounceSearch(initialValue = '', delayMs = 300) {
  const search = ref(initialValue)
  const debouncedSearch = refDebounced(search, delayMs)

  const clear = () => {
    search.value = ''
  }

  return {
    search,
    debouncedSearch,
    clear
  }
}
