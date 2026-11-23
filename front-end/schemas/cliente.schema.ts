import { z } from 'zod'
import { hasFullName } from '~/schemas/auth.schema'

export const CLIENTE_STATUS_OPTIONS = ['Ativo', 'Inativo'] as const

const onlyDigits = (value: string) => value.replace(/\D/g, '')

export const clienteSchema = z.object({
  cpfRg: z
    .string()
    .trim()
    .refine((val) => {
      const digits = onlyDigits(val)
      return digits.length === 11 || digits.length === 14
    }, 'Informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido.'),
  rg: z
    .string()
    .trim()
    .min(5, 'Informe um RG / IE válido (mín. 5 caracteres).')
    .max(12, 'O RG deve possuir no máximo 12 caracteres.'),
  nome: z
    .string()
    .trim()
    .min(3, 'Informe o nome completo ou razão social do cliente.')
    .max(60, 'O nome deve possuir no máximo 60 caracteres.')
    .refine(hasFullName, 'Informe o nome completo (nome e sobrenome) ou a razão social completa.'),
  empresaOpeCnpj: z
    .string()
    .trim()
    .min(1, 'Selecione a empresa operadora contratada.'),
  statusContrato: z.enum(CLIENTE_STATUS_OPTIONS),
  dataFormalizacao: z
    .string()
    .trim()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Informe uma data de formalização válida.'),
  demandaPrincipal: z
    .string()
    .trim()
    .max(80, 'Máximo de 80 caracteres.')
    .optional()
    .default(''),
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
    .min(1, 'Informe ao menos um telefone de contato.')
})

export type ClienteFormValues = z.infer<typeof clienteSchema>
