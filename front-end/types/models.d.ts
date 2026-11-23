export type StatusContrato = 'Ativo' | 'Inativo' | 'Suspenso'

export type StatusVoo = 'AGENDADO' | 'EM_ANDAMENTO' | 'CONCLUIDO' | 'CANCELADO'

export type NivelGravidade = 'BAIXO' | 'MEDIO' | 'ALTO' | 'CRITICO'

export type UserRole = 'ADMIN' | 'OPERADOR' | 'AUDITOR'

export interface AuthUser {
  id: string
  nome: string
  email: string
  role: UserRole
  empresaCnpj: string
  razaoSocial: string
  setorAtuacao: string
  telefone: string
  statusContrato: StatusContrato
  ativo: boolean
  criadoEm?: string
}

export interface LoginRequest {
  email: string
  senha: string
  manterConectado?: boolean
}

export interface RegisterRequest {
  nomeResponsavel: string
  razaoSocial: string
  cnpjCpf: string
  setorAtuacao: string
  telefone: string
  email: string
  senha: string
}

export interface ForgotPasswordRequest {
  email: string
}

export interface UpdateProfileRequest {
  nome: string
  razaoSocial: string
  setorAtuacao: string
  telefone: string
  email: string
  senhaAtual: string
}

export interface ChangePasswordRequest {
  senhaAtual: string
  novaSenha: string
}

export interface TokenResponse {
  token: string
  tipo?: string
  expiresIn?: number
  usuario: AuthUser
}

export interface EmpresaOperadora {
  cnpj: string
  razaoSocial: string
  setorAtuacao: string
  email: string
  telefones: string[]
  statusContrato: StatusContrato
}

export interface Drone {
  codigo: string
  fabricante: string
  modelo: string
  numSerie: string
  pesoMaxDecolagem: number
  tipoUso: 'basico' | 'avancado'
  dataValidadeCadastro: string
  empresaOpeCnpj: string
}

export interface Piloto {
  cpf: string
  rg: string
  nome: string
  email: string
  numRegistroLicenca: string
  validadeCma: string
  telefones: string[]
  empresaOpeCnpj: string
}

export interface Cliente {
  cpfRg: string
  rg: string
  nome: string
  empresaOpeCnpj: string
  statusContrato: StatusContrato
  dataFormalizacao: string
  telefones: string[]
}

export interface OperacaoVoo {
  codigo: number
  statusVoo: StatusVoo
  objetivoVoo: string
  dataHoraPlanejada: string
  dataHoraReal?: string | null
  origem: string
  destino: string
  descricaoArea: string
  tipoLocal: string
  droneCodigo: string
  pilotoCpf: string
  clienteCpfRg: string
  empresaOpeCnpj: string
}

export interface Auditoria {
  codigo: number
  operacaoVooCodigo: number
  dataHoraEvento: string
  categoria: string
  descricao: string
  nivelGravidade: NivelGravidade
}
