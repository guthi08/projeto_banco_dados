import { z } from 'zod'

export const SETORES_ATUACAO = [
  'Agricultura',
  'Geodésia',
  'Energia',
  'Construção Civil',
  'Segurança',
  'Meio Ambiente',
  'Logística',
  'Mídia',
  'Óleo e Gás',
  'Entretenimento'
] as const

const onlyDigits = (value: string) => value.replace(/\D/g, '')

export const hasFullName = (value: string) => {
  const parts = value.trim().split(/\s+/).filter((p) => p.length >= 2)
  return parts.length >= 2
}

export const strongPasswordSchema = z
  .string()
  .min(8, 'A senha deve ter no mínimo 8 caracteres.')
  .regex(/[A-Z]/, 'Inclua ao menos uma letra maiúscula.')
  .regex(/[a-z]/, 'Inclua ao menos uma letra minúscula.')
  .regex(/\d/, 'Inclua ao menos um número.')
  .regex(/[^A-Za-z0-9]/, 'Inclua ao menos um caractere especial (ex: @, #, !).')

export const loginSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, 'Informe o e-mail corporativo.')
    .email('Informe um endereço de e-mail válido.'),
  senha: z
    .string()
    .min(6, 'A senha deve possuir no mínimo 6 caracteres.'),
  manterConectado: z.boolean().optional().default(true)
})

export const registerSchema = z
  .object({
    nomeResponsavel: z
      .string()
      .trim()
      .min(3, 'Informe o nome completo do responsável (nome e sobrenome).')
      .max(80, 'Nome muito longo (máx. 80 caracteres).')
      .refine(hasFullName, 'Informe o nome completo do responsável (nome e sobrenome).'),
    razaoSocial: z
      .string()
      .trim()
      .min(3, 'Informe a razão social da operadora.')
      .max(80, 'Razão social muito longa (máx. 80 caracteres).'),
    cnpjCpf: z
      .string()
      .trim()
      .refine((val) => {
        const digits = onlyDigits(val)
        return digits.length === 11 || digits.length === 14
      }, 'Informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido.'),
    setorAtuacao: z
      .string()
      .trim()
      .min(1, 'Selecione o setor de atuação.'),
    telefone: z
      .string()
      .trim()
      .refine((val) => {
        const digits = onlyDigits(val)
        return digits.length === 10 || digits.length === 11
      }, 'Informe um telefone válido com DDD (10 ou 11 dígitos).'),
    email: z
      .string()
      .trim()
      .min(1, 'Informe o e-mail corporativo.')
      .email('Informe um endereço de e-mail válido.'),
    senha: strongPasswordSchema,
    confirmarSenha: z.string().min(1, 'Confirme a senha informada.')
  })
  .refine((data) => data.senha === data.confirmarSenha, {
    message: 'As senhas não conferem.',
    path: ['confirmarSenha']
  })

export const forgotPasswordSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, 'Informe o e-mail cadastrado.')
    .email('Informe um endereço de e-mail válido.')
})

export const profileSchema = z.object({
  nome: z
    .string()
    .trim()
    .min(3, 'Informe o nome completo (nome e sobrenome).')
    .max(80, 'Nome muito longo.')
    .refine(hasFullName, 'Informe o nome completo (nome e sobrenome).'),
  razaoSocial: z
    .string()
    .trim()
    .min(3, 'Informe a razão social da empresa operadora.')
    .max(80, 'Razão social muito longa.'),
  setorAtuacao: z
    .string()
    .trim()
    .min(1, 'Selecione o setor de atuação.'),
  telefone: z
    .string()
    .trim()
    .refine((val) => {
      const digits = onlyDigits(val)
      return digits.length === 10 || digits.length === 11
    }, 'Informe um telefone válido com DDD.'),
  email: z
    .string()
    .trim()
    .email('Informe um endereço de e-mail válido.'),
  senhaAtual: z
    .string()
    .min(6, 'Informe sua senha atual para confirmar a alteração do perfil.')
})

export const changePasswordSchema = z
  .object({
    senhaAtual: z.string().min(6, 'Informe sua senha atual.'),
    novaSenha: strongPasswordSchema,
    confirmarNovaSenha: z.string().min(1, 'Confirme a nova senha.')
  })
  .refine((data) => data.novaSenha === data.confirmarNovaSenha, {
    message: 'A confirmação da nova senha não confere.',
    path: ['confirmarNovaSenha']
  })
  .refine((data) => data.senhaAtual !== data.novaSenha, {
    message: 'A nova senha deve ser diferente da senha atual.',
    path: ['novaSenha']
  })

export type LoginFormValues = z.infer<typeof loginSchema>
export type RegisterFormValues = z.infer<typeof registerSchema>
export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>
export type ProfileFormValues = z.infer<typeof profileSchema>
export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>

export interface PasswordStrengthCriterion {
  key: string
  label: string
  met: boolean
}

export interface PasswordStrengthResult {
  score: 0 | 1 | 2 | 3 | 4
  label: string
  barClass: string
  textClass: string
  criteria: PasswordStrengthCriterion[]
}

export function evaluatePasswordStrength(password: string): PasswordStrengthResult {
  const criteria: PasswordStrengthCriterion[] = [
    { key: 'length', label: 'Mín. 8 caracteres', met: password.length >= 8 },
    { key: 'case', label: 'Maiúscula e minúscula', met: /[A-Z]/.test(password) && /[a-z]/.test(password) },
    { key: 'number', label: 'Ao menos 1 número', met: /\d/.test(password) },
    { key: 'symbol', label: 'Caractere especial', met: /[^A-Za-z0-9]/.test(password) }
  ]

  const score = criteria.filter((c) => c.met).length as 0 | 1 | 2 | 3 | 4

  if (!password) {
    return {
      score: 0,
      label: 'Aguardando senha',
      barClass: 'bg-slate-200',
      textClass: 'text-slate-500',
      criteria
    }
  }

  switch (score) {
    case 1:
      return {
        score,
        label: 'Muito fraca',
        barClass: 'bg-red-500',
        textClass: 'text-red-700',
        criteria
      }
    case 2:
      return {
        score,
        label: 'Fraca',
        barClass: 'bg-amber-500',
        textClass: 'text-amber-700',
        criteria
      }
    case 3:
      return {
        score,
        label: 'Boa',
        barClass: 'bg-blue-500',
        textClass: 'text-blue-700',
        criteria
      }
    case 4:
      return {
        score,
        label: 'Forte',
        barClass: 'bg-emerald-600',
        textClass: 'text-emerald-700',
        criteria
      }
    default:
      return {
        score: 0,
        label: 'Muito fraca',
        barClass: 'bg-red-500',
        textClass: 'text-red-700',
        criteria
      }
  }
}

export function formatCpfCnpj(raw: string): string {
  const digits = onlyDigits(raw).slice(0, 14)
  if (digits.length <= 11) {
    return digits
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d{1,2})$/, '$1-$2')
  }
  return digits
    .replace(/(\d{2})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1/$2')
    .replace(/(\d{4})(\d{1,2})$/, '$1-$2')
}

export function formatPhoneBR(raw: string): string {
  const digits = onlyDigits(raw).slice(0, 11)
  if (digits.length <= 10) {
    return digits
      .replace(/(\d{2})(\d)/, '($1) $2')
      .replace(/(\d{4})(\d{1,4})$/, '$1-$2')
  }
  return digits
    .replace(/(\d{2})(\d)/, '($1) $2')
    .replace(/(\d{5})(\d{1,4})$/, '$1-$2')
}
