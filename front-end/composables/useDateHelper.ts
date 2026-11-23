import dayjs from 'dayjs'

export type ValidityLevel = 'valid' | 'expiring' | 'expired'

export const useDateHelper = () => {
  const formatDateBR = (iso?: string | null): string => {
    if (!iso) return '—'
    const parsed = dayjs(iso)
    return parsed.isValid() ? parsed.format('DD/MM/YYYY') : iso
  }

  const formatDateTimeBR = (iso?: string | null): string => {
    if (!iso) return '—'
    const parsed = dayjs(iso)
    return parsed.isValid() ? parsed.format('DD/MM/YYYY HH:mm') : iso
  }

  const daysUntil = (iso?: string | null, referenceDate?: string): number => {
    if (!iso) return -1
    const target = dayjs(iso).startOf('day')
    const base = referenceDate ? dayjs(referenceDate).startOf('day') : dayjs().startOf('day')
    return target.diff(base, 'day')
  }

  const getValidityStatus = (iso?: string | null, warningThresholdDays = 30): ValidityLevel => {
    const remaining = daysUntil(iso)
    if (remaining < 0) return 'expired'
    if (remaining <= warningThresholdDays) return 'expiring'
    return 'valid'
  }

  return {
    dayjs,
    formatDateBR,
    formatDateTimeBR,
    daysUntil,
    getValidityStatus
  }
}
