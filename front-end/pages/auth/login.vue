<script setup lang="ts">
import type { FormSubmitEvent } from '#ui/types'
import { loginSchema, type LoginFormValues } from '~/schemas/auth.schema'

definePageMeta({
  layout: 'auth',
  middleware: 'guest'
})

useHead({
  title: 'Login — DRONE-MONITOR'
})

const route = useRoute()
const authStore = useAuthStore()
const toast = useToast()

const accessMode = ref<'operador' | 'admin'>('operador')
const showPassword = ref(false)
const errorMessage = ref<string | null>(null)
const registrationSuccess = ref(route.query.registered === '1')

const state = reactive<LoginFormValues>({
  email: typeof route.query.email === 'string' ? route.query.email : '',
  senha: '',
  manterConectado: true
})

const redirectTarget = computed(() => {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/'
})

const isAdminMode = computed(() => accessMode.value === 'admin')

function toggleAccessMode() {
  errorMessage.value = null
  accessMode.value = isAdminMode.value ? 'operador' : 'admin'
}

async function onSubmit(event: FormSubmitEvent<LoginFormValues>) {
  errorMessage.value = null
  try {
    if (isAdminMode.value) {
      await authStore.loginAdmin(event.data)
      toast.add({
        title: 'Acesso Administrador autorizado',
        description: `Bem-vindo(a), ${authStore.user?.nome || 'Administrador'}. Verificação completa liberada.`,
        icon: 'i-lucide-shield-check',
        color: 'blue'
      })
    } else {
      await authStore.login(event.data)
      toast.add({
        title: 'Acesso autorizado',
        description: `Bem-vindo(a), ${authStore.user?.nome || 'Operador'}.`,
        icon: 'i-lucide-shield-check',
        color: 'green'
      })
    }
    await navigateTo(redirectTarget.value)
  } catch (err) {
    if (err instanceof Error && err.message.startsWith('Acesso restrito:')) {
      errorMessage.value = err.message
    } else {
      errorMessage.value = extractApiErrorMessage(err, 'Não foi possível autenticar. Verifique suas credenciais.')
    }
  }
}
</script>

<template>
  <div>
    <div class="mb-6">
      <span
        class="code-mono inline-flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider"
        :class="isAdminMode ? 'text-indigo-600' : 'text-blue-600'"
      >
        <UIcon :name="isAdminMode ? 'i-lucide-shield-alert' : 'i-lucide-key-round'" class="h-3.5 w-3.5" />
        {{ isAdminMode ? 'Acesso Administrador · Verificação Completa' : 'Acesso do Operador · JWT' }}
      </span>
      <h1 class="mt-1 text-2xl font-bold tracking-tight text-slate-900">
        {{ isAdminMode ? 'Entrar como Administrador' : 'Entrar no painel' }}
      </h1>
      <p class="mt-1 text-sm text-slate-500">
        {{
          isAdminMode
            ? 'Informe o e-mail e a senha de administrador para auditar e verificar todo o sistema.'
            : 'Informe as credenciais da sua empresa operadora para gerenciar voos e auditorias.'
        }}
      </p>
    </div>

    <UAlert
      v-if="registrationSuccess"
      color="green"
      variant="soft"
      icon="i-lucide-check-circle-2"
      title="Cadastro realizado com sucesso!"
      description="Sua empresa operadora foi cadastrada. Faça login com seu e-mail e senha para acessar o painel."
      class="mb-5"
    />

    <UAlert
      v-if="errorMessage"
      color="red"
      variant="soft"
      icon="i-lucide-alert-circle"
      title="Falha na autenticação"
      :description="errorMessage"
      class="mb-5"
    />

    <UForm :schema="loginSchema" :state="state" class="space-y-4" @submit="onSubmit">
      <UFormGroup :label="isAdminMode ? 'E-mail do administrador' : 'E-mail corporativo'" name="email" required>
        <UInput
          v-model="state.email"
          type="email"
          :placeholder="isAdminMode ? 'admin@empresa.com.br' : 'operacoes@empresa.com.br'"
          icon="i-lucide-mail"
          size="lg"
          autocomplete="email"
        />
      </UFormGroup>

      <UFormGroup label="Senha" name="senha" required>
        <UInput
          v-model="state.senha"
          :type="showPassword ? 'text' : 'password'"
          placeholder="Digite sua senha"
          icon="i-lucide-lock"
          size="lg"
          autocomplete="current-password"
          :ui="{ icon: { trailing: { pointer: '' } } }"
        >
          <template #trailing>
            <UButton
              color="gray"
              variant="link"
              :icon="showPassword ? 'i-lucide-eye-off' : 'i-lucide-eye'"
              :padded="false"
              aria-label="Alternar visibilidade da senha"
              @click="showPassword = !showPassword"
            />
          </template>
        </UInput>
      </UFormGroup>

      <div class="flex items-center justify-between gap-2 pt-1">
        <UCheckbox v-model="state.manterConectado" label="Manter sessão ativa" />
        <NuxtLink
          to="/auth/forgot-password"
          class="text-xs font-semibold text-blue-600 hover:text-blue-700 hover:underline"
        >
          Esqueceu a senha?
        </NuxtLink>
      </div>

      <UButton
        type="submit"
        :color="isAdminMode ? 'indigo' : 'blue'"
        size="lg"
        block
        :loading="authStore.loading"
        :icon="isAdminMode ? 'i-lucide-shield-check' : 'i-lucide-log-in'"
        class="mt-2"
      >
        {{ isAdminMode ? 'Entrar como Administrador' : 'Entrar no Painel' }}
      </UButton>
    </UForm>

    <div class="my-5 flex items-center gap-3">
      <div class="h-px flex-1 bg-slate-200" />
      <span class="code-mono text-[11px] uppercase tracking-wider text-slate-400">ou</span>
      <div class="h-px flex-1 bg-slate-200" />
    </div>

    <UButton
      color="gray"
      variant="outline"
      size="md"
      block
      :icon="isAdminMode ? 'i-lucide-user' : 'i-lucide-shield'"
      @click="toggleAccessMode"
    >
      {{ isAdminMode ? 'Voltar para Acesso do Operador' : 'Acesso Administrador' }}
    </UButton>

    <p v-if="!isAdminMode" class="mt-6 text-center text-sm text-slate-600">
      Ainda não possui conta?
      <NuxtLink to="/auth/register" class="font-semibold text-blue-600 hover:text-blue-700 hover:underline">
        Cadastrar operadora
      </NuxtLink>
    </p>
  </div>
</template>
