import { z } from 'zod'

export const AUDITORIA_CATEGORIAS = [
  'Desvio de Rota',
  'Incidente Aéreo',
  'Alerta Meteorológico',
  'Falha de Comunicação',
  'Conformidade Pré-Voo',
  'Inspeção de Segurança',
  'Restrição de Espaço Aéreo'
] as const

export const GRAVIDADE_OPTIONS = [
  { label: 'Baixo', value: 'BAIXO' as const },
  { label: 'Médio', value: 'MEDIO' as const },
  { label: 'Alto', value: 'ALTO' as const },
  { label: 'Crítico', value: 'CRITICO' as const }
]

export const auditoriaSchema = z.object({
  operacaoVooCodigo: z.coerce
    .number({ invalid_type_error: 'Selecione a operação de voo vinculada.' })
    .int()
    .positive('Selecione uma operação de voo válida.'),
  dataHoraEvento: z
    .string()
    .trim()
    .min(10, 'Informe a data e hora da ocorrência.'),
  categoria: z
    .string()
    .trim()
    .min(3, 'Selecione ou informe a categoria do evento.')
    .max(60, 'Máximo de 60 caracteres.'),
  nivelGravidade: z.enum(['BAIXO', 'MEDIO', 'ALTO', 'CRITICO'], {
    errorMap: () => ({ message: 'Selecione o nível de gravidade.' })
  }),
  descricao: z
    .string()
    .trim()
    .min(15, 'Descreva a ocorrência com detalhes (mín. 15 caracteres).')
    .max(500, 'Máximo de 500 caracteres.')
})

export type AuditoriaFormValues = z.infer<typeof auditoriaSchema>
