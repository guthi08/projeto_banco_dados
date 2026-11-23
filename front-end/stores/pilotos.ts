import dayjs from 'dayjs'
import { defineStore } from 'pinia'
import type { Piloto } from '~/types/models'
import type { PageResponse } from '~/types/api'

export interface PilotoRecord extends Piloto {
  voosRealizados?: number
}

const normalizeDigits = (value: string) => value.replace(/\D/g, '')

export const usePilotosStore = defineStore('pilotos', () => {
  const authStore = useAuthStore()
  const { getValidityStatus, daysUntil } = useDateHelper()

  const pilotos = ref<PilotoRecord[]>([])
  const loading = ref(false)
  const initialized = ref(false)

  const stats = computed(() => {
    let validos = 0
    let vencendo = 0
    let vencidos = 0

    for (const p of pilotos.value) {
      const level = getValidityStatus(p.validadeCma, 30)
      if (level === 'expired') vencidos += 1
      else if (level === 'expiring') vencendo += 1
      else validos += 1
    }

    const total = pilotos.value.length
    const aptos = validos + vencendo
    return {
      total,
      validos,
      vencendo,
      vencidos,
      aptos,
      aptosPct: total > 0 ? Math.round((aptos / total) * 100) : 0
    }
  })

  function isEligibleForFlight(piloto: PilotoRecord, flightDateIso?: string): boolean {
    const refDate = flightDateIso || dayjs().format('YYYY-MM-DD')
    return daysUntil(piloto.validadeCma, refDate) >= 0
  }

  async function fetchPilotos(force = false) {
    if (!authStore.isAuthenticated) return
    if (initialized.value && !force) return

    loading.value = true
    try {
      const api = useApi()
      const ownDigits = normalizeDigits(authStore.user?.empresaCnpj || '')
      const query = !authStore.isAdmin && ownDigits ? `?size=100&empresaCnpj=${ownDigits}` : '?size=100'
      const res = await api<PageResponse<Piloto> | Piloto[]>(`/pilotos${query}`)
      const list = Array.isArray(res) ? res : res.content
      pilotos.value = (list || []).map((item) => ({
        ...item,
        telefones: item.telefones?.length ? [...item.telefones] : []
      }))
      initialized.value = true
    } finally {
      loading.value = false
    }
  }

  async function createPiloto(payload: Piloto): Promise<PilotoRecord> {
    loading.value = true
    try {
      const api = useApi()
      const saved = await api<Piloto>('/pilotos', {
        method: 'POST',
        body: payload
      })

      const created: PilotoRecord = {
        ...saved,
        telefones: saved.telefones?.length ? [...saved.telefones] : [],
        voosRealizados: 0
      }

      pilotos.value.unshift(created)
      return created
    } finally {
      loading.value = false
    }
  }

  async function updatePiloto(originalCpf: string, payload: Piloto): Promise<PilotoRecord> {
    loading.value = true
    try {
      const targetDigits = normalizeDigits(originalCpf)
      const api = useApi()
      const saved = await api<Piloto>(`/pilotos/${encodeURIComponent(targetDigits)}`, {
        method: 'PUT',
        body: payload
      })

      const idx = pilotos.value.findIndex((p) => normalizeDigits(p.cpf) === targetDigits)
      const updated: PilotoRecord = {
        ...(idx !== -1 ? pilotos.value[idx] : {}),
        ...saved,
        telefones: saved.telefones?.length ? [...saved.telefones] : []
      }
      if (idx !== -1) {
        pilotos.value[idx] = updated
      } else {
        pilotos.value.unshift(updated)
      }
      return updated
    } finally {
      loading.value = false
    }
  }

  async function renewCma(cpf: string, months = 12): Promise<PilotoRecord> {
    const targetDigits = normalizeDigits(cpf)
    const current = pilotos.value.find((p) => normalizeDigits(p.cpf) === targetDigits)
    if (!current) {
      throw new Error('Piloto não encontrado.')
    }

    const baseDate =
      daysUntil(current.validadeCma) > 0 ? dayjs(current.validadeCma) : dayjs()
    const newValidity = baseDate.add(months, 'month').format('YYYY-MM-DD')

    return await updatePiloto(cpf, {
      ...current,
      validadeCma: newValidity
    })
  }

  async function removePiloto(cpf: string): Promise<void> {
    loading.value = true
    try {
      const targetDigits = normalizeDigits(cpf)
      const api = useApi()
      await api(`/pilotos/${encodeURIComponent(targetDigits)}`, {
        method: 'DELETE'
      })
      pilotos.value = pilotos.value.filter((p) => normalizeDigits(p.cpf) !== targetDigits)
    } finally {
      loading.value = false
    }
  }

  function reset() {
    pilotos.value = []
    initialized.value = false
  }

  return {
    pilotos,
    loading,
    stats,
    isEligibleForFlight,
    fetchPilotos,
    createPiloto,
    updatePiloto,
    renewCma,
    removePiloto,
    reset
  }
})
