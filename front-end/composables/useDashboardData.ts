import dayjs from 'dayjs'
import type { NivelGravidade, StatusVoo } from '~/types/models'
import type { PeriodFilter } from '~/stores/filters'

export interface DashboardOperation {
  codigo: number
  code: string
  droneCode: string
  droneModel: string
  pilotName: string
  pilotCpf: string
  clientName: string
  objective: string
  originDesc: string
  destDesc: string
  plannedAt: string
  actualAt?: string | null
  status: StatusVoo
}

export interface ValidityAlertItem {
  id: string
  level: 'critical' | 'warning'
  kind: 'pilot' | 'drone'
  targetCode: string
  title: string
  subtitle: string
  expiresAt: string
  daysRemaining: number
  route: string
}

export interface DashboardAuditItem {
  id: string
  operacaoVooCodigo: number
  operationCode: string
  pilotName: string
  timestamp: string
  category: string
  description: string
  severity: NivelGravidade
}

export interface MonthlyFlightSeries {
  labels: string[]
  planned: number[]
  completed: number[]
}

export const useDashboardData = () => {
  const filtersStore = useFiltersStore()
  const empresasStore = useEmpresasStore()
  const clientesStore = useClientesStore()
  const dronesStore = useDronesStore()
  const pilotosStore = usePilotosStore()
  const operacoesStore = useOperacoesStore()
  const auditoriasStore = useAuditoriasStore()
  const { daysUntil, formatDateBR } = useDateHelper()

  const loading = computed(
    () =>
      dronesStore.loading ||
      pilotosStore.loading ||
      operacoesStore.loading ||
      auditoriasStore.loading
  )

  const drones = computed(() =>
    dronesStore.drones.map((d) => ({
      code: d.codigo,
      model: `${d.fabricante} ${d.modelo}`,
      validUntil: d.dataValidadeCadastro,
      inMaintenance: Boolean(d.emManutencao)
    }))
  )

  const pilots = computed(() =>
    pilotosStore.pilotos.map((p) => ({
      cpf: p.cpf,
      name: p.nome,
      cmaValidUntil: p.validadeCma
    }))
  )

  const operations = computed<DashboardOperation[]>(() =>
    operacoesStore.operacoes.map((op) => {
      const resolved = operacoesStore.resolveLabels(op)
      return {
        codigo: op.codigo,
        code: op.displayCode,
        droneCode: op.droneCodigo,
        droneModel: resolved.droneModel,
        pilotName: resolved.pilotName,
        pilotCpf: op.pilotoCpf,
        clientName: resolved.clientName,
        objective: op.objetivoVoo,
        originDesc: op.origem,
        destDesc: op.destino,
        plannedAt: op.dataHoraPlanejada,
        actualAt: op.dataHoraReal,
        status: op.statusVoo
      }
    })
  )

  const audits = computed<DashboardAuditItem[]>(() =>
    auditoriasStore.auditorias.map((a) => ({
      id: a.displayId,
      operacaoVooCodigo: a.operacaoVooCodigo,
      operationCode: a.operacaoDisplayCode,
      pilotName: a.pilotoNome,
      timestamp: a.dataHoraEvento,
      category: a.categoria,
      description: a.descricao,
      severity: a.nivelGravidade
    }))
  )

  async function refresh(force = true) {
    await Promise.allSettled([
      empresasStore.fetchEmpresas(force),
      clientesStore.fetchClientes(force),
      dronesStore.fetchDrones(force),
      pilotosStore.fetchPilotos(force),
      operacoesStore.fetchOperacoes(force),
      auditoriasStore.fetchAuditorias(force)
    ])
  }

  function matchesPeriod(isoDate: string, period: PeriodFilter): boolean {
    if (period === 'all') return true
    const daysMap: Record<Exclude<PeriodFilter, 'all'>, number> = {
      '7d': 7,
      '30d': 30,
      '90d': 90
    }
    const diff = dayjs().startOf('day').diff(dayjs(isoDate).startOf('day'), 'day')
    return diff <= daysMap[period]
  }

  const periodOperations = computed(() =>
    operations.value.filter((op) => matchesPeriod(op.plannedAt, filtersStore.period))
  )

  const validityAlerts = computed<ValidityAlertItem[]>(() => {
    const items: ValidityAlertItem[] = []

    for (const pilot of pilots.value) {
      const remaining = daysUntil(pilot.cmaValidUntil)
      if (remaining < 0) {
        items.push({
          id: `AL-PIL-${pilot.cpf}`,
          level: 'critical',
          kind: 'pilot',
          targetCode: pilot.cpf,
          title: `CMA Vencido — ${pilot.name}`,
          subtitle: `Venceu em ${formatDateBR(pilot.cmaValidUntil)} · Bloqueado para escalas.`,
          expiresAt: pilot.cmaValidUntil,
          daysRemaining: remaining,
          route: '/pilotos'
        })
      } else if (remaining <= 30) {
        items.push({
          id: `AL-PIL-${pilot.cpf}`,
          level: 'warning',
          kind: 'pilot',
          targetCode: pilot.cpf,
          title: `CMA Próximo do Vencimento — ${pilot.name}`,
          subtitle: `Vence em ${remaining} dias (${formatDateBR(pilot.cmaValidUntil)}).`,
          expiresAt: pilot.cmaValidUntil,
          daysRemaining: remaining,
          route: '/pilotos'
        })
      }
    }

    for (const drone of drones.value) {
      const remaining = daysUntil(drone.validUntil)
      if (remaining < 0) {
        items.push({
          id: `AL-DRN-${drone.code}`,
          level: 'critical',
          kind: 'drone',
          targetCode: drone.code,
          title: `Cadastro ANAC Expirado — ${drone.code}`,
          subtitle: `${drone.model} · Venceu em ${formatDateBR(drone.validUntil)}.`,
          expiresAt: drone.validUntil,
          daysRemaining: remaining,
          route: '/drones'
        })
      } else if (remaining <= 30) {
        items.push({
          id: `AL-DRN-${drone.code}`,
          level: 'warning',
          kind: 'drone',
          targetCode: drone.code,
          title: `Validade ANAC Próxima — ${drone.code}`,
          subtitle: `${drone.model} · Vence em ${remaining} dias (${formatDateBR(drone.validUntil)}).`,
          expiresAt: drone.validUntil,
          daysRemaining: remaining,
          route: '/drones'
        })
      }
    }

    return items.sort((a, b) => a.daysRemaining - b.daysRemaining)
  })

  const statusCounts = computed<Record<StatusVoo, number>>(() => {
    const counts: Record<StatusVoo, number> = {
      AGENDADO: 0,
      EM_ANDAMENTO: 0,
      CONCLUIDO: 0,
      CANCELADO: 0
    }

    for (const op of periodOperations.value) {
      counts[op.status] = (counts[op.status] || 0) + 1
    }

    return counts
  })

  const monthlySeries = computed<MonthlyFlightSeries>(() => {
    const labels: string[] = []
    const planned: number[] = []
    const completed: number[] = []

    const monthKeys: string[] = []
    for (let offset = 5; offset >= 0; offset -= 1) {
      const refMonth = dayjs().subtract(offset, 'month')
      const rawLabel = refMonth.format('MMM/YY')
      labels.push(rawLabel.charAt(0).toUpperCase() + rawLabel.slice(1))
      monthKeys.push(refMonth.format('YYYY-MM'))
    }

    monthKeys.forEach((key) => {
      const opsInMonth = operations.value.filter(
        (op) => dayjs(op.plannedAt).format('YYYY-MM') === key
      )
      const plannedCount = opsInMonth.length
      const completedCount = opsInMonth.filter((op) => op.status === 'CONCLUIDO').length

      planned.push(plannedCount)
      completed.push(completedCount)
    })

    return { labels, planned, completed }
  })

  const kpis = computed(() => {
    const activeFlights = operations.value.filter((op) => op.status === 'EM_ANDAMENTO').length
    const totalDrones = drones.value.length
    const availableDrones = drones.value.filter(
      (d) => !d.inMaintenance && daysUntil(d.validUntil) >= 0
    ).length
    const availablePct = totalDrones > 0 ? Math.round((availableDrones / totalDrones) * 100) : 0
    const criticalAudits = audits.value.filter(
      (a) => a.severity === 'CRITICO' || a.severity === 'ALTO'
    ).length

    return {
      activeFlights,
      availableDrones,
      totalDrones,
      availablePct,
      alertsCount: validityAlerts.value.length,
      criticalAlertsCount: validityAlerts.value.filter((a) => a.level === 'critical').length,
      auditsCount: audits.value.length,
      criticalAudits
    }
  })

  return {
    loading,
    drones,
    pilots,
    operations,
    periodOperations,
    audits,
    validityAlerts,
    statusCounts,
    monthlySeries,
    kpis,
    refresh
  }
}
