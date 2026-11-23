<script setup lang="ts">
import type { FormSubmitEvent } from '#ui/types'
import { forgotPasswordSchema, type ForgotPasswordFormValues } from '~/schemas/auth.schema'

definePageMeta({
  layout: 'auth',
  middleware: 'guest'
})

useHead({
  title: 'Recuperar Senha — DRONE-MONITOR'
})

const authStore = useAuthStore()

const submitted = ref(false)
const submittedEmail = ref('')
const errorMessage = ref<string | null>(null)

const state = reactive<ForgotPasswordFormValues>({
  email: ''
})

async function onSubmit(event: FormSubmitEvent<ForgotPasswordFormValues>) {
  errorMessage.value = null
  try {
    await authStore.forgotPassword(event.data)
    submittedEmail.value = event.data.email
    submitted.value = true
  } catch (err) {
    errorMessage.value = extractApiErrorMessage(err, 'Não foi possível enviar as instruções de recuperação.')
  }
}

function resetFlow() {
  submitted.value = false
  errorMessage.value = null
  state.email = ''
}
</script>

<template>
  <div>
    <NuxtLink
      to="/auth/login"
      class="mb-5 inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-800"
    >
      <UIcon name="i-lucide-arrow-left" class="h-3.5 w-3.5" />
      <span>Voltar ao login</span>
    </NuxtLink>

    <template v-if="!submitted">
      <div class="mb-6">
        <span class="code-mono text-[11px] font-semibold uppercase tracking-wider text-blue-600">
          Recuperação de Acesso
        </span>
        <h1 class="mt-1 text-2xl font-bold tracking-tight text-slate-900">
          Esqueceu sua senha?
        </h1>
        <p class="mt-1 text-sm text-slate-500">
          Informe o e-mail corporativo vinculado à sua conta. Enviaremos um link temporário para redefinição.
        </p>
      </div>

      <UAlert
        v-if="errorMessage"
        color="red"
        variant="soft"
        icon="i-lucide-alert-circle"
        title="Não foi possível enviar"
        :description="errorMessage"
        class="mb-5"
      />

      <UForm :schema="forgotPasswordSchema" :state="state" class="space-y-4" @submit="onSubmit">
        <UFormGroup label="E-mail cadastrado" name="email" required>
          <UInput
            v-model="state.email"
            type="email"
            placeholder="operacoes@empresa.com.br"
            icon="i-lucide-mail"
            size="lg"
            autocomplete="email"
          />
        </UFormGroup>

        <UButton
          type="submit"
          color="blue"
          size="lg"
          block
          :loading="authStore.loading"
          icon="i-lucide-send"
        >
          Enviar instruções de recuperação
        </UButton>
      </UForm>
    </template>

    <template v-else>
      <div class="rounded-xl border border-emerald-200 bg-emerald-50 p-5 text-center">
        <span class="mx-auto inline-flex h-11 w-11 items-center justify-center rounded-full bg-emerald-600 text-white">
          <UIcon name="i-lucide-mail-check" class="h-5 w-5" />
        </span>

        <h1 class="mt-3 text-lg font-bold text-slate-900">
          Instruções enviadas
        </h1>
        <p class="mt-1.5 text-sm leading-relaxed text-slate-600">
          Caso exista uma conta ativa para
          <strong class="code-mono text-slate-900">{{ submittedEmail }}</strong>,
          você receberá um e-mail com o link de redefinição nos próximos minutos.
        </p>

        <div class="mt-5 flex flex-col gap-2">
          <UButton to="/auth/login" color="blue" size="md" block>
            Retornar ao Login
          </UButton>
          <UButton color="gray" variant="ghost" size="sm" block @click="resetFlow">
            Informar outro e-mail
          </UButton>
        </div>
      </div>
    </template>
  </div>
</template>
