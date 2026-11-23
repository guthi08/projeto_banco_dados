import { defineStore } from 'pinia'
import type {
  AuthUser,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  TokenResponse,
  UpdateProfileRequest
} from '~/types/models'
import type { ApiMessageResponse } from '~/types/api'

const LEGACY_DEMO_TOKEN = 'demo-jwt-aero-monitor-session'

export const useAuthStore = defineStore('auth', () => {
  const isHttps = import.meta.client ? window.location.protocol === 'https:' : false

  const tokenCookie = useCookie<string | null>('aero_monitor_token', {
    default: () => null,
    sameSite: 'lax',
    secure: isHttps,
    maxAge: 60 * 60 * 8 // 8 horas
  })

  const userCookie = useCookie<AuthUser | null>('aero_monitor_user', {
    default: () => null,
    sameSite: 'lax',
    secure: isHttps,
    maxAge: 60 * 60 * 8
  })

  const adminModeCookie = useCookie<boolean>('aero_monitor_admin_mode', {
    default: () => false,
    sameSite: 'lax',
    secure: isHttps,
    maxAge: 60 * 60 * 8
  })

  // Invalida imediatamente qualquer cookie antigo do extinto modo demonstração
  if (tokenCookie.value === LEGACY_DEMO_TOKEN) {
    tokenCookie.value = null
    userCookie.value = null
    adminModeCookie.value = false
  }

  const loading = ref(false)

  const token = computed(() => (tokenCookie.value === LEGACY_DEMO_TOKEN ? null : tokenCookie.value))
  const user = computed(() => (token.value ? userCookie.value : null))
  const isAuthenticated = computed(() => Boolean(token.value && user.value))
  const isAdmin = computed(() => user.value?.role === 'ADMIN' && Boolean(adminModeCookie.value))

  const userInitials = computed(() => {
    const nome = user.value?.nome?.trim()
    if (!nome) return 'OP'
    const parts = nome.split(/\s+/).filter(Boolean)
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
    return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase()
  })

  const operatorName = computed(() => user.value?.razaoSocial || 'Operadora não vinculada')

  function setSession(newToken: string, newUser: AuthUser, asAdmin = false) {
    tokenCookie.value = newToken
    userCookie.value = newUser
    adminModeCookie.value = asAdmin && newUser.role === 'ADMIN'
  }

  async function login(payload: LoginRequest): Promise<AuthUser> {
    loading.value = true
    try {
      const api = useApi()
      const response = await api<TokenResponse>('/auth/login', {
        method: 'POST',
        body: {
          email: payload.email,
          senha: payload.senha
        }
      })

      setSession(response.token, response.usuario, false)
      return response.usuario
    } finally {
      loading.value = false
    }
  }

  async function loginAdmin(payload: LoginRequest): Promise<AuthUser> {
    loading.value = true
    try {
      const api = useApi()
      const response = await api<TokenResponse>('/auth/login', {
        method: 'POST',
        body: {
          email: payload.email,
          senha: payload.senha
        }
      })

      if (response.usuario?.role !== 'ADMIN') {
        throw new Error('Acesso restrito: as credenciais informadas não pertencem a um perfil Administrador (ADMIN).')
      }

      setSession(response.token, response.usuario, true)
      return response.usuario
    } finally {
      loading.value = false
    }
  }

  async function register(payload: RegisterRequest): Promise<TokenResponse> {
    loading.value = true
    try {
      const api = useApi()
      return await api<TokenResponse>('/auth/register', {
        method: 'POST',
        body: payload
      })
    } finally {
      loading.value = false
    }
  }

  async function forgotPassword(payload: ForgotPasswordRequest): Promise<ApiMessageResponse> {
    loading.value = true
    try {
      const api = useApi()
      return await api<ApiMessageResponse>('/auth/forgot-password', {
        method: 'POST',
        body: payload
      })
    } finally {
      loading.value = false
    }
  }

  async function fetchProfile(): Promise<AuthUser | null> {
    if (!token.value) return null

    loading.value = true
    try {
      const api = useApi()
      const profile = await api<AuthUser>('/account/profile', {
        method: 'GET'
      })
      userCookie.value = profile
      return profile
    } finally {
      loading.value = false
    }
  }

  async function updateProfile(payload: UpdateProfileRequest): Promise<AuthUser> {
    loading.value = true
    try {
      const api = useApi()
      const updated = await api<AuthUser>('/account/profile', {
        method: 'PUT',
        body: payload
      })
      userCookie.value = updated
      return updated
    } finally {
      loading.value = false
    }
  }

  async function changePassword(payload: ChangePasswordRequest): Promise<ApiMessageResponse> {
    loading.value = true
    try {
      const api = useApi()
      return await api<ApiMessageResponse>('/account/password', {
        method: 'PATCH',
        body: payload
      })
    } finally {
      loading.value = false
    }
  }

  async function deactivateAccount(senhaAtual: string): Promise<void> {
    loading.value = true
    try {
      const api = useApi()
      await api('/account/deactivate', {
        method: 'PATCH',
        body: { senhaAtual }
      })
      logout({ redirect: true })
    } finally {
      loading.value = false
    }
  }

  function logout(options: { redirect?: boolean } = { redirect: true }) {
    tokenCookie.value = null
    userCookie.value = null
    adminModeCookie.value = false

    useEmpresasStore().reset()
    useDronesStore().reset()
    usePilotosStore().reset()
    useClientesStore().reset()
    useOperacoesStore().reset()
    useAuditoriasStore().reset()

    if (options.redirect && import.meta.client) {
      navigateTo('/auth/login')
    }
  }

  return {
    token,
    user,
    loading,
    isAuthenticated,
    isAdmin,
    userInitials,
    operatorName,
    login,
    loginAdmin,
    register,
    forgotPassword,
    fetchProfile,
    updateProfile,
    changePassword,
    deactivateAccount,
    logout
  }
})
