import { z } from 'zod'
import { hasFullName } from '~/schemas/auth.schema'

const onlyDigits = (value: string) => value.replace(/\D/g, '')

export const pilotoSchema = z.object({
  cpf: z
    .string()
    .trim()
    .refine((val) => onlyDigits(val).length === 11, 'Informe um CPF válido com 11 dígitos.'),
  rg: z
    .string()
    .trim()
    .min(5, 'Informe um RG válido (mín. 5 caracteres).')
    .max(12, 'O RG deve possuir no máximo 12 caracteres.'),
  nome: z
    .string()
    .trim()
    .min(3, 'Informe o nome completo do piloto (nome e sobrenome).')
    .max(45, 'O nome deve possuir no máximo 45 caracteres.')
    .refine(hasFullName, 'Informe o nome completo do piloto (nome e sobrenome).'),
  email: z
    .string()
    .trim()
    .min(1, 'Informe o e-mail corporativo.')
    .email('Informe um endereço de e-mail válido.'),
  numRegistroLicenca: z
    .string()
    .trim()
    .transform((v) => v.toUpperCase())
    .refine(
      (val) => val.length >= 5 && val.length <= 20,
      'Informe o registro/licença de voo (5 a 20 caracteres, ex: ANAC-RPAS-88412).'
    ),
  validadeCma: z
    .string()
    .trim()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Informe uma data de validade do CMA válida.'),
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
  empresaOpeCnpj: z
    .string()
    .trim()
    .min(1, 'Selecione a empresa operadora vinculada ao piloto.')
})

export type PilotoFormValues = z.infer<typeof pilotoSchema>

export function formatCpf(raw: string): string {
  const digits = onlyDigits(raw).slice(0, 11)
  return digits
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2')
}

export function formatLicenseCode(raw: string): string {
  return raw.toUpperCase().replace(/[^A-Z0-9-]/g, '').slice(0, 20)
}
