import { z } from 'zod'

export const TIPOS_LOCAL = [
  'Rural',
  'Urbano',
  'Industrial',
  'Misto',
  'Infraestrutura Crítica',
  'Área de Risco'
] as const

export const operacaoVooSchema = z
  .object({
    empresaOpeCnpj: z
      .string()
      .trim()
      .min(1, 'Selecione a empresa operadora responsável.'),
    droneCodigo: z
      .string()
      .trim()
      .min(1, 'Selecione uma aeronave (drone) regularizada na ANAC.'),
    pilotoCpf: z
      .string()
      .trim()
      .min(1, 'Selecione um piloto com CMA válido.'),
    clienteCpfRg: z
      .string()
      .trim()
      .min(1, 'Selecione o cliente solicitante da missão.'),
    objetivoVoo: z
      .string()
      .trim()
      .min(5, 'Descreva o objetivo do voo (mín. 5 caracteres).')
      .max(120, 'Máximo de 120 caracteres.'),
    origem: z
      .string()
      .trim()
      .min(3, 'Informe o local de origem da decolagem.'),
    destino: z
      .string()
      .trim()
      .min(3, 'Informe o local de destino / pouso.'),
    descricaoArea: z
      .string()
      .trim()
      .min(3, 'Descreva a área sobrevoada.')
      .max(120, 'Máximo de 120 caracteres.'),
    tipoLocal: z
      .string()
      .trim()
      .min(1, 'Selecione o tipo de local de operação.'),
    dataHoraPlanejada: z
      .string()
      .trim()
      .min(10, 'Informe a data e hora planejadas para a decolagem.'),
    duracaoEstimadaMin: z.coerce
      .number({ invalid_type_error: 'Informe a duração estimada em minutos.' })
      .int('Use minutos inteiros.')
      .min(15, 'Duração mínima de 15 minutos.')
      .max(480, 'Duração máxima de 480 minutos (8 horas).')
  })
  .refine(
    (data) => data.origem.trim().toLowerCase() !== data.destino.trim().toLowerCase(),
    {
      message: 'Origem e destino devem ser dois locais de operação distintos.',
      path: ['destino']
    }
  )

export type OperacaoVooFormValues = z.infer<typeof operacaoVooSchema>
