import dayjs from 'dayjs'
import { defineStore } from 'pinia'
import type { OperacaoVoo, StatusVoo } from '~/types/models'
import type { PageResponse } from '~/types/api'

export interface OperacaoVooRecord extends OperacaoVoo {
  displayCode: string
  duracaoEstimadaMin: number
  dataHoraPousoReal?: string | null
  droneModelo?: string
  pilotoNome?: string
  clienteNome?: string
}

export interface PreFlightCheckResult {
  valid: boolean
  errors: string[]
  warnings: string[]
}

interface OperacaoApiResponse extends OperacaoVoo {
  duracaoEstimadaMin?: number
  dataHoraPousoReal?: string | null
  droneModelo?: string
  pilotoNome?: string
  clienteNome?: string
}

const normalizeDigits = (val: string) => val.replace(/\D/g, '')

function mapToRecord(item: OperacaoApiResponse): OperacaoVooRecord {
  const year = dayjs(item.dataHoraPlanejada).isValid()
    ? dayjs(item.dataHoraPlanejada).year()
    : dayjs().year()
  return {
    ...item,
    displayCode: `VO-${year}-${String(item.codigo).padStart(3, '0')}`,
    duracaoEstimadaMin: item.duracaoEstimadaMin || 60,
    dataHoraPousoReal: item.dataHoraPousoReal ?? null
  }
}

export const useOperacoesStore = defineStore('operacoes', () => {
  const authStore = useAuthStore()
  const dronesStore = useDronesStore()
  const pilotosStore = usePilotosStore()
  const clientesStore = useClientesStore()
  const { daysUntil, formatDateBR, formatDateTimeBR } = useDateHelper()

  const operacoes = ref<OperacaoVooRecord[]>([])
  const loading = ref(false)
  const initialized = ref(false)

  const stats = computed(() => {
    const counts: Record<StatusVoo, number> = {
      AGENDADO: 0,
      EM_ANDAMENTO: 0,
      CONCLUIDO: 0,
      CANCELADO: 0
    }
    for (const op of operacoes.value) {
      counts[op.statusVoo] = (counts[op.statusVoo] || 0) + 1
    }
    return {
      total: operacoes.value.length,
      ...counts
    }
  })

  function resolveLabels(op: OperacaoVooRecord) {
    const drone = dronesStore.drones.find((d) => d.codigo.toUpperCase() === op.droneCodigo.toUpperCase())
    const piloto = pilotosStore.pilotos.find(
      (p) => normalizeDigits(p.cpf) === normalizeDigits(op.pilotoCpf)
    )
    const cliente = clientesStore.clientes.find(
      (c) => normalizeDigits(c.cpfRg) === normalizeDigits(op.clienteCpfRg)
    )

    return {
      droneModel: op.droneModelo || (drone ? `${drone.fabricante} ${drone.modelo}` : op.droneCodigo),
      pilotName: op.pilotoNome || piloto?.nome || op.pilotoCpf,
      clientName: op.clienteNome || cliente?.nome || op.clienteCpfRg,
      drone,
      piloto,
      cliente
    }
  }

  function getByCodigo(codigo: number): OperacaoVooRecord | null {
    return operacoes.value.find((op) => op.codigo === codigo) || null
  }

  function validatePreFlight(
    payload: {
      droneCodigo: string
      pilotoCpf: string
      clienteCpfRg: string
      dataHoraPlanejada: string
      duracaoEstimadaMin: number
    },
    excludeCodigo?: number
  ): PreFlightCheckResult {
    const errors: string[] = []
    const warnings: string[] = []

    const flightStart = dayjs(payload.dataHoraPlanejada)
    if (!flightStart.isValid()) {
      errors.push('Data/hora planejada inválida.')
      return { valid: false, errors, warnings }
    }
    const flightDateIso = flightStart.format('YYYY-MM-DD')
    const flightEnd = flightStart.add(payload.duracaoEstimadaMin || 60, 'minute')

    // 1. Trava ANAC do Drone
    const drone = dronesStore.drones.find(
      (d) => d.codigo.toUpperCase() === payload.droneCodigo.toUpperCase()
    )
    if (!drone) {
      errors.push('Aeronave selecionada não encontrada na frota.')
    } else {
      if (drone.emManutencao) {
        errors.push(`Aeronave ${drone.codigo} está em manutenção e não pode ser escalada.`)
      }
      const droneDays = daysUntil(drone.dataValidadeCadastro, flightDateIso)
      if (droneDays < 0) {
        errors.push(
          `Trava ANAC: o drone ${drone.codigo} tem cadastro vencido para a data do voo (validade ${formatDateBR(drone.dataValidadeCadastro)}).`
        )
      } else if (droneDays <= 30) {
        warnings.push(
          `Atenção ANAC: cadastro do drone ${drone.codigo} vence em ${droneDays} dia(s) após a missão.`
        )
      }
    }

    // 2. Trava CMA do Piloto
    const piloto = pilotosStore.pilotos.find(
      (p) => normalizeDigits(p.cpf) === normalizeDigits(payload.pilotoCpf)
    )
    if (!piloto) {
      errors.push('Piloto selecionado não encontrado.')
    } else {
      const cmaDays = daysUntil(piloto.validadeCma, flightDateIso)
      if (cmaDays < 0) {
        errors.push(
          `Trava CMA: o piloto ${piloto.nome} está com o CMA expirado para a data do voo (validade ${formatDateBR(piloto.validadeCma)}).`
        )
      } else if (cmaDays <= 30) {
        warnings.push(
          `Atenção CMA: o certificado médico de ${piloto.nome} vence em ${cmaDays} dia(s) após a missão.`
        )
      }
    }

    // 3. Contrato do Cliente
    const cliente = clientesStore.clientes.find(
      (c) => normalizeDigits(c.cpfRg) === normalizeDigits(payload.clienteCpfRg)
    )
    if (cliente && cliente.statusContrato !== 'Ativo') {
      errors.push(`O cliente ${cliente.nome} está com contrato ${cliente.statusContrato} e não pode solicitar voos.`)
    }

    // 4. Bloqueio de Conflito de Horário (AGENDADO ou EM_ANDAMENTO)
    for (const existing of operacoes.value) {
      if (excludeCodigo && existing.codigo === excludeCodigo) continue
      if (existing.statusVoo !== 'AGENDADO' && existing.statusVoo !== 'EM_ANDAMENTO') continue

      const existingStart = dayjs(existing.dataHoraPlanejada)
      const existingEnd = existingStart.add(existing.duracaoEstimadaMin || 60, 'minute')

      const overlaps = flightStart.isBefore(existingEnd) && flightEnd.isAfter(existingStart)
      if (!overlaps) continue

      if (existing.droneCodigo.toUpperCase() === payload.droneCodigo.toUpperCase()) {
        errors.push(
          `Conflito de agenda: o drone ${payload.droneCodigo} já está alocado na operação ${existing.displayCode} (${formatDateTimeBR(existing.dataHoraPlanejada)}).`
        )
      }
      if (normalizeDigits(existing.pilotoCpf) === normalizeDigits(payload.pilotoCpf)) {
        errors.push(
          `Conflito de agenda: o piloto selecionado já está escalado na operação ${existing.displayCode} (${formatDateTimeBR(existing.dataHoraPlanejada)}).`
        )
      }
    }

    return {
      valid: errors.length === 0,
      errors,
      warnings
    }
  }

  async function fetchOperacoes(force = false) {
    if (!authStore.isAuthenticated) return
    if (initialized.value && !force) return

    loading.value = true
    try {
      const api = useApi()
      const ownDigits = normalizeDigits(authStore.user?.empresaCnpj || '')
      const query = !authStore.isAdmin && ownDigits ? `?size=100&empresaCnpj=${ownDigits}` : '?size=100'
      const res = await api<PageResponse<OperacaoApiResponse> | OperacaoApiResponse[]>(`/operacoes${query}`)
      const list = Array.isArray(res) ? res : res.content
      operacoes.value = (list || []).map(mapToRecord)
      initialized.value = true
    } finally {
      loading.value = false
    }
  }

  async function createOperacao(payload: {
    empresaOpeCnpj: string
    droneCodigo: string
    pilotoCpf: string
    clienteCpfRg: string
    objetivoVoo: string
    origem: string
    destino: string
    descricaoArea: string
    tipoLocal: string
    dataHoraPlanejada: string
    duracaoEstimadaMin: number
  }): Promise<OperacaoVooRecord> {
    const check = validatePreFlight(payload)
    if (!check.valid) {
      throw new Error(check.errors[0])
    }

    loading.value = true
    try {
      const api = useApi()
      const saved = await api<OperacaoApiResponse>('/operacoes', {
        method: 'POST',
        body: payload
      })

      const created = mapToRecord(saved)
      operacoes.value.unshift(created)
      return created
    } finally {
      loading.value = false
    }
  }

  async function updateStatus(codigo: number, nextStatus: StatusVoo): Promise<OperacaoVooRecord> {
    const idx = operacoes.value.findIndex((op) => op.codigo === codigo)
    const current = idx !== -1 ? operacoes.value[idx] : null

    if (current && nextStatus === 'EM_ANDAMENTO') {
      const check = validatePreFlight(
        {
          droneCodigo: current.droneCodigo,
          pilotoCpf: current.pilotoCpf,
          clienteCpfRg: current.clienteCpfRg,
          dataHoraPlanejada: dayjs().format('YYYY-MM-DDTHH:mm'),
          duracaoEstimadaMin: current.duracaoEstimadaMin
        },
        current.codigo
      )
      if (!check.valid) {
        throw new Error(check.errors[0])
      }
    }

    const api = useApi()
    const saved = await api<OperacaoApiResponse>(`/operacoes/${codigo}/status`, {
      method: 'PATCH',
      body: { statusVoo: nextStatus }
    })

    const updated = mapToRecord(saved)
    if (idx !== -1) {
      operacoes.value[idx] = updated
    } else {
      operacoes.value.unshift(updated)
    }
    return updated
  }

  function reset() {
    operacoes.value = []
    initialized.value = false
  }

  return {
    operacoes,
    loading,
    stats,
    resolveLabels,
    getByCodigo,
    validatePreFlight,
    fetchOperacoes,
    createOperacao,
    updateStatus,
    reset
  }
})
