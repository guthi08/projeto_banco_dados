import { defineStore } from 'pinia'
import type { Cliente, StatusContrato } from '~/types/models'
import type { PageResponse } from '~/types/api'

export interface ClienteRecord extends Cliente {
  demandaPrincipal?: string
}

const normalizeDigits = (value: string) => value.replace(/\D/g, '')

export const useClientesStore = defineStore('clientes', () => {
  const authStore = useAuthStore()

  const clientes = ref<ClienteRecord[]>([])
  const loading = ref(false)
  const initialized = ref(false)

  const stats = computed(() => {
    const total = clientes.value.length
    const ativos = clientes.value.filter((c) => c.statusContrato === 'Ativo').length
    const inativos = total - ativos
    const operadorasVinculadas = new Set(clientes.value.map((c) => c.empresaOpeCnpj)).size
    return {
      total,
      ativos,
      inativos,
      operadorasVinculadas
    }
  })

  async function fetchClientes(force = false) {
    if (!authStore.isAuthenticated) return
    if (initialized.value && !force) return

    loading.value = true
    try {
      const api = useApi()
      const ownDigits = normalizeDigits(authStore.user?.empresaCnpj || '')
      const query = !authStore.isAdmin && ownDigits ? `?size=100&empresaCnpj=${ownDigits}` : '?size=100'
      const res = await api<PageResponse<Cliente> | Cliente[]>(`/clientes${query}`)
      const list = Array.isArray(res) ? res : res.content
      clientes.value = (list || []).map((item) => ({
        ...item,
        telefones: item.telefones?.length ? [...item.telefones] : []
      }))
      initialized.value = true
    } finally {
      loading.value = false
    }
  }

  async function createCliente(payload: ClienteRecord): Promise<ClienteRecord> {
    loading.value = true
    try {
      const api = useApi()
      const saved = await api<Cliente>('/clientes', {
        method: 'POST',
        body: payload
      })

      const created: ClienteRecord = {
        ...saved,
        demandaPrincipal: payload.demandaPrincipal,
        telefones: saved.telefones?.length ? [...saved.telefones] : []
      }
      clientes.value.unshift(created)
      return created
    } finally {
      loading.value = false
    }
  }

  async function updateCliente(originalCpfRg: string, payload: ClienteRecord): Promise<ClienteRecord> {
    loading.value = true
    try {
      const targetDigits = normalizeDigits(originalCpfRg)
      const api = useApi()
      const saved = await api<Cliente>(`/clientes/${encodeURIComponent(targetDigits)}`, {
        method: 'PUT',
        body: payload
      })

      const idx = clientes.value.findIndex((c) => normalizeDigits(c.cpfRg) === targetDigits)
      const updated: ClienteRecord = {
        ...(idx !== -1 ? clientes.value[idx] : {}),
        ...saved,
        demandaPrincipal: payload.demandaPrincipal,
        telefones: saved.telefones?.length ? [...saved.telefones] : []
      }
      if (idx !== -1) {
        clientes.value[idx] = updated
      } else {
        clientes.value.unshift(updated)
      }
      return updated
    } finally {
      loading.value = false
    }
  }

  async function toggleContratoStatus(cpfRg: string): Promise<ClienteRecord> {
    const targetDigits = normalizeDigits(cpfRg)
    const idx = clientes.value.findIndex((c) => normalizeDigits(c.cpfRg) === targetDigits)
    if (idx === -1) {
      throw new Error('Cliente não encontrado.')
    }

    const nextStatus: StatusContrato = clientes.value[idx].statusContrato === 'Ativo' ? 'Inativo' : 'Ativo'
    const api = useApi()
    const saved = await api<Cliente>(`/clientes/${encodeURIComponent(targetDigits)}/status`, {
      method: 'PATCH',
      body: { statusContrato: nextStatus }
    })

    const updated: ClienteRecord = {
      ...clientes.value[idx],
      ...saved
    }
    clientes.value[idx] = updated
    return updated
  }

  async function removeCliente(cpfRg: string): Promise<void> {
    loading.value = true
    try {
      const targetDigits = normalizeDigits(cpfRg)
      const api = useApi()
      await api(`/clientes/${encodeURIComponent(targetDigits)}`, {
        method: 'DELETE'
      })
      clientes.value = clientes.value.filter((c) => normalizeDigits(c.cpfRg) !== targetDigits)
    } finally {
      loading.value = false
    }
  }

  function reset() {
    clientes.value = []
    initialized.value = false
  }

  return {
    clientes,
    loading,
    stats,
    fetchClientes,
    createCliente,
    updateCliente,
    toggleContratoStatus,
    removeCliente,
    reset
  }
})
