import { defineStore } from 'pinia'
import type { EmpresaOperadora, StatusContrato } from '~/types/models'
import type { PageResponse } from '~/types/api'

export interface EmpresaOperadoraRecord extends EmpresaOperadora {
  dronesCount: number
  pilotosCount: number
  clientesCount: number
  dataContrato: string
  responsavel?: string
}

const normalizeDigits = (value: string) => value.replace(/\D/g, '')

export const useEmpresasStore = defineStore('empresas', () => {
  const authStore = useAuthStore()

  const empresas = ref<EmpresaOperadoraRecord[]>([])
  const activeOperatorCnpj = ref<string>(authStore.user?.empresaCnpj || '')
  const loading = ref(false)
  const initialized = ref(false)

  const activeOperator = computed(() => {
    const targetDigits = normalizeDigits(activeOperatorCnpj.value || authStore.user?.empresaCnpj || '')
    return (
      empresas.value.find((e) => normalizeDigits(e.cnpj) === targetDigits) ||
      empresas.value[0] ||
      null
    )
  })

  const stats = computed(() => {
    const total = empresas.value.length
    const ativas = empresas.value.filter((e) => e.statusContrato === 'Ativo').length
    const inativasOuSuspensas = total - ativas
    const setoresUnicos = new Set(empresas.value.map((e) => e.setorAtuacao)).size
    return {
      total,
      ativas,
      inativasOuSuspensas,
      setoresUnicos
    }
  })

  function mapToRecord(item: EmpresaOperadora, existing?: EmpresaOperadoraRecord): EmpresaOperadoraRecord {
    return {
      ...item,
      telefones: item.telefones?.length ? [...item.telefones] : [],
      dronesCount: existing?.dronesCount ?? 0,
      pilotosCount: existing?.pilotosCount ?? 0,
      clientesCount: existing?.clientesCount ?? 0,
      dataContrato: existing?.dataContrato ?? new Date().toISOString().slice(0, 10),
      responsavel: existing?.responsavel
    }
  }

  async function fetchEmpresas(force = false) {
    if (!authStore.isAuthenticated) return
    if (initialized.value && !force) return

    loading.value = true
    try {
      const api = useApi()
      const res = await api<PageResponse<EmpresaOperadora> | EmpresaOperadora[]>('/empresas?size=100')
      const list = Array.isArray(res) ? res : res.content
      const ownDigits = normalizeDigits(authStore.user?.empresaCnpj || '')
      const scoped = !authStore.isAdmin && ownDigits
        ? (list || []).filter((item) => normalizeDigits(item.cnpj) === ownDigits)
        : (list || [])

      empresas.value = scoped.map((item) => mapToRecord(item))
      if (!activeOperatorCnpj.value && (authStore.user?.empresaCnpj || empresas.value[0]?.cnpj)) {
        activeOperatorCnpj.value = authStore.user?.empresaCnpj || empresas.value[0].cnpj
      }
      initialized.value = true
    } finally {
      loading.value = false
    }
  }

  async function createEmpresa(payload: EmpresaOperadora): Promise<EmpresaOperadoraRecord> {
    if (!authStore.isAdmin) {
      throw new Error('Apenas administradores podem cadastrar novas empresas operadoras.')
    }
    loading.value = true
    try {
      const api = useApi()
      const saved = await api<EmpresaOperadora>('/empresas', {
        method: 'POST',
        body: payload
      })

      const created = mapToRecord(saved)
      empresas.value.unshift(created)
      return created
    } finally {
      loading.value = false
    }
  }

  async function updateEmpresa(originalCnpj: string, payload: EmpresaOperadora): Promise<EmpresaOperadoraRecord> {
    loading.value = true
    try {
      const targetDigits = normalizeDigits(originalCnpj)
      const api = useApi()
      const saved = await api<EmpresaOperadora>(`/empresas/${encodeURIComponent(targetDigits)}`, {
        method: 'PUT',
        body: payload
      })

      const idx = empresas.value.findIndex((e) => normalizeDigits(e.cnpj) === targetDigits)
      const updated = mapToRecord(saved, idx !== -1 ? empresas.value[idx] : undefined)

      if (idx !== -1) {
        empresas.value[idx] = updated
      } else {
        empresas.value.unshift(updated)
      }

      if (normalizeDigits(activeOperatorCnpj.value) === targetDigits) {
        activeOperatorCnpj.value = updated.cnpj
      }

      return updated
    } finally {
      loading.value = false
    }
  }

  async function setStatus(cnpj: string, status: StatusContrato): Promise<EmpresaOperadoraRecord> {
    const targetDigits = normalizeDigits(cnpj)
    const api = useApi()
    const saved = await api<EmpresaOperadora>(`/empresas/${encodeURIComponent(targetDigits)}/status`, {
      method: 'PATCH',
      body: { statusContrato: status }
    })

    const idx = empresas.value.findIndex((e) => normalizeDigits(e.cnpj) === targetDigits)
    const updated = mapToRecord(saved, idx !== -1 ? empresas.value[idx] : undefined)
    if (idx !== -1) {
      empresas.value[idx] = updated
    }
    return updated
  }

  async function toggleStatus(cnpj: string): Promise<EmpresaOperadoraRecord> {
    const targetDigits = normalizeDigits(cnpj)
    const current = empresas.value.find((e) => normalizeDigits(e.cnpj) === targetDigits)
    if (!current) {
      throw new Error('Empresa operadora não encontrada.')
    }
    const nextStatus: StatusContrato = current.statusContrato === 'Ativo' ? 'Inativo' : 'Ativo'
    return await setStatus(cnpj, nextStatus)
  }

  function setActiveOperator(cnpj: string) {
    activeOperatorCnpj.value = cnpj
  }

  function reset() {
    empresas.value = []
    activeOperatorCnpj.value = ''
    initialized.value = false
  }

  return {
    empresas,
    activeOperatorCnpj,
    activeOperator,
    loading,
    stats,
    fetchEmpresas,
    createEmpresa,
    updateEmpresa,
    setStatus,
    toggleStatus,
    setActiveOperator,
    reset
  }
})
