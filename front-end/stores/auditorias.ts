import dayjs from 'dayjs'
import { defineStore } from 'pinia'
import type { Auditoria, NivelGravidade } from '~/types/models'
import type { PageResponse } from '~/types/api'

export interface AuditoriaRecord extends Auditoria {
  displayId: string
  operacaoDisplayCode: string
  pilotoNome: string
  empresaOpeCnpj: string
  empresaRazaoSocial: string
}

interface AuditoriaApiResponse extends Auditoria {
  pilotoNome?: string
  droneCodigo?: string
  empresaOpeCnpj?: string
  empresaRazaoSocial?: string
}

const normalizeDigits = (val: string) => val.replace(/\D/g, '')

export const useAuditoriasStore = defineStore('auditorias', () => {
  const authStore = useAuthStore()
  const operacoesStore = useOperacoesStore()
  const empresasStore = useEmpresasStore()

  const auditorias = ref<AuditoriaRecord[]>([])
  const loading = ref(false)
  const initialized = ref(false)

  const stats = computed(() => {
    const counts: Record<NivelGravidade, number> = {
      BAIXO: 0,
      MEDIO: 0,
      ALTO: 0,
      CRITICO: 0
    }
    for (const a of auditorias.value) {
      counts[a.nivelGravidade] = (counts[a.nivelGravidade] || 0) + 1
    }
    const categoriasUnicas = new Set(auditorias.value.map((a) => a.categoria)).size
    return {
      total: auditorias.value.length,
      ...counts,
      categoriasUnicas
    }
  })

  const byCategory = computed(() => {
    const map = new Map<string, number>()
    for (const a of auditorias.value) {
      map.set(a.categoria, (map.get(a.categoria) || 0) + 1)
    }
    return Array.from(map.entries())
      .map(([categoria, count]) => ({ categoria, count }))
      .sort((a, b) => b.count - a.count)
  })

  function mapToRecord(item: AuditoriaApiResponse): AuditoriaRecord {
    const op = operacoesStore.getByCodigo(item.operacaoVooCodigo)
    const resolved = op ? operacoesStore.resolveLabels(op) : null
    const year = dayjs(item.dataHoraEvento).isValid()
      ? dayjs(item.dataHoraEvento).year()
      : dayjs().year()

    const cnpj = item.empresaOpeCnpj || op?.empresaOpeCnpj || ''
    const empresaFound = cnpj
      ? empresasStore.empresas.find((e) => normalizeDigits(e.cnpj) === normalizeDigits(cnpj))
      : null

    return {
      ...item,
      displayId: `AUD-${year}-${String(item.codigo).padStart(3, '0')}`,
      operacaoDisplayCode: op?.displayCode || `VO-${year}-${String(item.operacaoVooCodigo).padStart(3, '0')}`,
      pilotoNome: item.pilotoNome || resolved?.pilotName || '—',
      empresaOpeCnpj: cnpj,
      empresaRazaoSocial: item.empresaRazaoSocial || empresaFound?.razaoSocial || cnpj || '—'
    }
  }

  function getByOperation(operacaoVooCodigo: number): AuditoriaRecord[] {
    return auditorias.value.filter((a) => a.operacaoVooCodigo === operacaoVooCodigo)
  }

  async function fetchAuditorias(force = false) {
    if (!authStore.isAuthenticated) return
    if (initialized.value && !force) return

    loading.value = true
    try {
      const api = useApi()
      const ownDigits = normalizeDigits(authStore.user?.empresaCnpj || '')
      const query = !authStore.isAdmin && ownDigits ? `?size=100&empresaCnpj=${ownDigits}` : '?size=100'
      const res = await api<PageResponse<AuditoriaApiResponse> | AuditoriaApiResponse[]>(`/auditorias${query}`)
      const list = Array.isArray(res) ? res : res.content
      auditorias.value = (list || []).map(mapToRecord)
      initialized.value = true
    } finally {
      loading.value = false
    }
  }

  async function createAuditoria(payload: {
    operacaoVooCodigo: number
    dataHoraEvento: string
    categoria: string
    nivelGravidade: NivelGravidade
    descricao: string
  }): Promise<AuditoriaRecord> {
    loading.value = true
    try {
      const api = useApi()
      const saved = await api<AuditoriaApiResponse>('/auditorias', {
        method: 'POST',
        body: payload
      })

      const created = mapToRecord(saved)
      auditorias.value.unshift(created)
      return created
    } finally {
      loading.value = false
    }
  }

  function reset() {
    auditorias.value = []
    initialized.value = false
  }

  return {
    auditorias,
    loading,
    stats,
    byCategory,
    getByOperation,
    fetchAuditorias,
    createAuditoria,
    reset
  }
})
