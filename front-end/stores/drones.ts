import dayjs from 'dayjs'
import { defineStore } from 'pinia'
import type { Drone } from '~/types/models'
import type { PageResponse } from '~/types/api'

export interface DroneRecord extends Drone {
  emManutencao?: boolean
  ultimaInspecao?: string
}

export const useDronesStore = defineStore('drones', () => {
  const authStore = useAuthStore()
  const { getValidityStatus, daysUntil } = useDateHelper()

  const drones = ref<DroneRecord[]>([])
  const loading = ref(false)
  const initialized = ref(false)

  const stats = computed(() => {
    let validos = 0
    let vencendo = 0
    let vencidos = 0
    let disponiveis = 0

    for (const drone of drones.value) {
      const level = getValidityStatus(drone.dataValidadeCadastro, 30)
      if (level === 'expired') {
        vencidos += 1
      } else if (level === 'expiring') {
        vencendo += 1
        if (!drone.emManutencao) disponiveis += 1
      } else {
        validos += 1
        if (!drone.emManutencao) disponiveis += 1
      }
    }

    const total = drones.value.length
    return {
      total,
      validos,
      vencendo,
      vencidos,
      disponiveis,
      disponiveisPct: total > 0 ? Math.round((disponiveis / total) * 100) : 0
    }
  })

  function isEligibleForFlight(drone: DroneRecord, flightDateIso?: string): boolean {
    if (drone.emManutencao) return false
    const refDate = flightDateIso || dayjs().format('YYYY-MM-DD')
    return daysUntil(drone.dataValidadeCadastro, refDate) >= 0
  }

  async function fetchDrones(force = false) {
    if (!authStore.isAuthenticated) return
    if (initialized.value && !force) return

    loading.value = true
    try {
      const api = useApi()
      const ownDigits = (authStore.user?.empresaCnpj || '').replace(/\D/g, '')
      const query = !authStore.isAdmin && ownDigits ? `?size=100&empresaCnpj=${ownDigits}` : '?size=100'
      const res = await api<PageResponse<Drone> | Drone[]>(`/drones${query}`)
      const list = Array.isArray(res) ? res : res.content
      drones.value = (list || []).map((item) => ({
        ...item,
        emManutencao: false
      }))
      initialized.value = true
    } finally {
      loading.value = false
    }
  }

  async function createDrone(payload: Drone): Promise<DroneRecord> {
    loading.value = true
    try {
      const normalizedCode = payload.codigo.toUpperCase()
      const api = useApi()
      const saved = await api<Drone>('/drones', {
        method: 'POST',
        body: { ...payload, codigo: normalizedCode }
      })

      const created: DroneRecord = {
        ...saved,
        emManutencao: false,
        ultimaInspecao: dayjs().format('YYYY-MM-DD')
      }

      drones.value.unshift(created)
      return created
    } finally {
      loading.value = false
    }
  }

  async function updateDrone(originalCode: string, payload: Drone): Promise<DroneRecord> {
    loading.value = true
    try {
      const api = useApi()
      const saved = await api<Drone>(`/drones/${encodeURIComponent(originalCode)}`, {
        method: 'PUT',
        body: { ...payload, codigo: payload.codigo.toUpperCase() }
      })

      const idx = drones.value.findIndex((d) => d.codigo.toUpperCase() === originalCode.toUpperCase())
      const updated: DroneRecord = {
        ...(idx !== -1 ? drones.value[idx] : {}),
        ...saved
      }
      if (idx !== -1) {
        drones.value[idx] = updated
      } else {
        drones.value.unshift(updated)
      }
      return updated
    } finally {
      loading.value = false
    }
  }

  async function renewAnacRegistration(codigo: string, months = 12): Promise<DroneRecord> {
    const idx = drones.value.findIndex((d) => d.codigo.toUpperCase() === codigo.toUpperCase())
    if (idx === -1) {
      throw new Error('Aeronave não encontrada.')
    }

    const current = drones.value[idx]
    const baseDate =
      daysUntil(current.dataValidadeCadastro) > 0
        ? dayjs(current.dataValidadeCadastro)
        : dayjs()
    const newValidity = baseDate.add(months, 'month').format('YYYY-MM-DD')

    return await updateDrone(codigo, {
      ...current,
      dataValidadeCadastro: newValidity
    })
  }

  async function toggleMaintenance(codigo: string): Promise<DroneRecord> {
    const idx = drones.value.findIndex((d) => d.codigo.toUpperCase() === codigo.toUpperCase())
    if (idx === -1) {
      throw new Error('Aeronave não encontrada.')
    }
    drones.value[idx] = {
      ...drones.value[idx],
      emManutencao: !drones.value[idx].emManutencao
    }
    return drones.value[idx]
  }

  async function removeDrone(codigo: string): Promise<void> {
    loading.value = true
    try {
      const api = useApi()
      await api(`/drones/${encodeURIComponent(codigo)}`, {
        method: 'DELETE'
      })
      drones.value = drones.value.filter((d) => d.codigo.toUpperCase() !== codigo.toUpperCase())
    } finally {
      loading.value = false
    }
  }

  function reset() {
    drones.value = []
    initialized.value = false
  }

  return {
    drones,
    loading,
    stats,
    isEligibleForFlight,
    fetchDrones,
    createDrone,
    updateDrone,
    renewAnacRegistration,
    toggleMaintenance,
    removeDrone,
    reset
  }
})
