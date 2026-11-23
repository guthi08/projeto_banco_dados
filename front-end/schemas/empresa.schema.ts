import { z } from 'zod'
import { SETORES_ATUACAO, hasFullName } from '~/schemas/auth.schema'

export const STATUS_CONTRATO_OPTIONS = ['Ativo', 'Inativo', 'Suspenso'] as const

const onlyDigits = (value: string) => value.replace(/\D/g, '')

export const empresaSchema = z
  .object({
    cnpj: z
      .string()
      .trim()
      .refine((val) => {
        const digits = onlyDigits(val)
        return digits.length === 11 || digits.length === 14
      }, 'Informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido.'),
    razaoSocial: z
      .string()
      .trim()
      .min(3, 'Informe a razão social ou nome completo do responsável.')
      .max(80, 'A razão social deve possuir no máximo 80 caracteres.'),
    setorAtuacao: z
      .string()
      .trim()
      .min(1, 'Selecione o setor de atuação.'),
    email: z
      .string()
      .trim()
      .min(1, 'Informe o e-mail corporativo.')
      .email('Informe um endereço de e-mail válido.'),
    telefones: z
      .array(
        z
          .string()
          .trim()
          .refine((val) => {
            const digits = onlyDigits(val)
            return digits.length === 10 || digits.length === 11
          }, 'Informe um telefone válido com DDD (10 ou 11 dígitos).')
      )
      .min(1, 'Informe ao menos um telefone de contato.'),
    statusContrato: z.enum(STATUS_CONTRATO_OPTIONS)
  })
  .refine(
    (data) => onlyDigits(data.cnpj).length !== 11 || hasFullName(data.razaoSocial),
    {
      message: 'Para cadastro com CPF, informe o nome completo do responsável (nome e sobrenome).',
      path: ['razaoSocial']
    }
  )

export type EmpresaFormValues = z.infer<typeof empresaSchema>

export { SETORES_ATUACAO }
