import type { ApiErrorResponse } from '~/types/api'

export const useApi = () => {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()

  return $fetch.create({
    baseURL: config.public.apiBaseUrl || 'http://localhost:8080/api/v1',
    onRequest({ options }) {
      if (authStore.token) {
        const headers = new Headers(options.headers as HeadersInit | undefined)
        headers.set('Authorization', `Bearer ${authStore.token}`)
        options.headers = headers
      }
    },
    onResponseError({ response }) {
      if (response.status === 401) {
        authStore.logout({ redirect: false })
        if (import.meta.client) {
          navigateTo('/auth/login')
        }
      }
    }
  })
}

export function extractApiErrorMessage(err: unknown, fallback = 'Não foi possível concluir a solicitação.'): string {
  if (!err || typeof err !== 'object') {
    return fallback
  }

  const fetchErr = err as {
    data?: Partial<ApiErrorResponse>
    statusCode?: number
    message?: string
  }

  if (fetchErr.data?.message) {
    return fetchErr.data.message
  }

  if (fetchErr.statusCode === 0 || fetchErr.message?.includes('fetch failed') || fetchErr.message?.includes('Failed to fetch')) {
    return 'API Back-end indisponível em http://localhost:8080/api/v1. Verifique se o servidor Spring Boot está em execução.'
  }

  if (fetchErr.statusCode === 401) {
    return 'E-mail ou senha inválidos.'
  }

  if (fetchErr.statusCode === 403) {
    return 'Você não possui permissão para executar esta ação.'
  }

  if (fetchErr.statusCode === 409) {
    return 'Já existe um cadastro com os dados informados (e-mail ou CNPJ/CPF).'
  }

  return fallback
}
