import { z } from 'zod'

export const ANAC_CODE_REGEX = /^(PR|PP|PS)-[A-Z0-9]{3,6}$/

export const TIPO_USO_OPTIONS = [
  { label: 'Básico (VLOS / até 25kg)', value: 'basico' as const },
  { label: 'Avançado (BVLOS / Especial)', value: 'avancado' as const }
]

export const FABRICANTES_SUGERIDOS = [
  'DJI',
  'XAG',
  'Autel',
  'Flyability',
  'Skydio',
  'Parrot',
  'AeroDelivery'
] as const

export const droneSchema = z.object({
  codigo: z
    .string()
    .trim()
    .transform((v) => v.toUpperCase())
    .refine(
      (val) => ANAC_CODE_REGEX.test(val),
      'Use o padrão aeronáutico brasileiro: PR-XXX, PP-XXX ou PS-XXX (ex: PR-401).'
    ),
  fabricante: z
    .string()
    .trim()
    .min(2, 'Informe o fabricante da aeronave.')
    .max(45, 'Máximo de 45 caracteres.'),
  modelo: z
    .string()
    .trim()
    .min(2, 'Informe o modelo da aeronave.')
    .max(45, 'Máximo de 45 caracteres.'),
  numSerie: z
    .string()
    .trim()
    .min(4, 'Informe o número de série (mín. 4 caracteres).')
    .max(45, 'Máximo de 45 caracteres.'),
  pesoMaxDecolagem: z.coerce
    .number({ invalid_type_error: 'Informe um peso numérico válido em kg.' })
    .positive('O peso máximo de decolagem deve ser maior que zero.')
    .max(999.99, 'O peso máximo não pode exceder 999,99 kg.'),
  tipoUso: z.enum(['basico', 'avancado'], {
    errorMap: () => ({ message: 'Selecione o tipo de uso (básico ou avançado).' })
  }),
  dataValidadeCadastro: z
    .string()
    .trim()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Informe uma data de validade ANAC válida.'),
  empresaOpeCnpj: z
    .string()
    .trim()
    .min(1, 'Selecione a empresa operadora responsável pela aeronave.')
})

export type DroneFormValues = z.infer<typeof droneSchema>

export function formatAnacCode(raw: string): string {
  const cleaned = raw.toUpperCase().replace(/[^A-Z0-9-]/g, '')
  if (cleaned.length >= 2 && !cleaned.includes('-') && cleaned.length > 2) {
    return `${cleaned.slice(0, 2)}-${cleaned.slice(2, 8)}`
  }
  return cleaned.slice(0, 9)
}
