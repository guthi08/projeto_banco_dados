<script setup lang="ts">
import type { FormSubmitEvent } from '#ui/types'
import {
  SETORES_ATUACAO,
  formatCpfCnpj,
  formatPhoneBR,
  registerSchema,
  type RegisterFormValues
} from '~/schemas/auth.schema'

definePageMeta({
  layout: 'auth',
  middleware: 'guest'
})

useHead({
  title: 'Cadastro de Operadora — DRONE-MONITOR'
})

const authStore = useAuthStore()
const toast = useToast()

const showPassword = ref(false)
const showConfirmPassword = ref(false)
const errorMessage = ref<string | null>(null)

const state = reactive<RegisterFormValues>({
  nomeResponsavel: '',
  razaoSocial: '',
  cnpjCpf: '',
  setorAtuacao: '',
  telefone: '',
  email: '',
  senha: '',
  confirmarSenha: ''
})

function onDocInput(value: string) {
  state.cnpjCpf = formatCpfCnpj(value)
}

function onPhoneInput(value: string) {
  state.telefone = formatPhoneBR(value)
}

async function onSubmit(event: FormSubmitEvent<RegisterFormValues>) {
  errorMessage.value = null
  try {
    await authStore.register({
      nomeResponsavel: event.data.nomeResponsavel,
      razaoSocial: event.data.razaoSocial,
      cnpjCpf: event.data.cnpjCpf,
      setorAtuacao: event.data.setorAtuacao,
      telefone: event.data.telefone,
      email: event.data.email,
      senha: event.data.senha
    })

    toast.add({
      title: 'Cadastro realizado com sucesso!',
      description: 'Sua operadora foi cadastrada. Faça login com seu e-mail e senha para acessar o painel.',
      icon: 'i-lucide-check-circle-2',
      color: 'green'
    })

    await navigateTo({
      path: '/auth/login',
      query: {
        registered: '1',
        email: event.data.email.trim().toLowerCase()
      }
    })
  } catch (err) {
    errorMessage.value = extractApiErrorMessage(err, 'Não foi possível concluir o cadastro da operadora.')
  }
}
</script>

<template>
  <div>
    <div class="mb-5">
      <span class="code-mono text-[11px] font-semibold uppercase tracking-wider text-blue-600">
        Credenciamento · Nova Operadora
      </span>
      <h1 class="mt-1 text-2xl font-bold tracking-tight text-slate-900">
        Criar conta operacional
      </h1>
      <p class="mt-1 text-sm text-slate-500">
        Cadastre sua empresa operadora para controlar frota ANAC, pilotos e missões.
      </p>
    </div>

    <UAlert
      v-if="errorMessage"
      color="red"
      variant="soft"
      icon="i-lucide-alert-circle"
      title="Erro no cadastro"
      :description="errorMessage"
      class="mb-4"
    />

    <UForm :schema="registerSchema" :state="state" class="space-y-3.5" @submit="onSubmit">
      <UFormGroup
        label="Nome completo do responsável"
        description="Informe nome e sobrenome completos."
        name="nomeResponsavel"
        required
      >
        <UInput
          v-model="state.nomeResponsavel"
          placeholder="Ex: Carlos Eduardo Silva"
          icon="i-lucide-user"
          size="md"
        />
      </UFormGroup>

      <UFormGroup label="Razão social / Operadora" name="razaoSocial" required>
        <UInput
          v-model="state.razaoSocial"
          placeholder="Ex: AeroLog Brasil Ltda"
          icon="i-lucide-building-2"
          size="md"
        />
      </UFormGroup>

      <div class="grid gap-3.5 sm:grid-cols-2">
        <UFormGroup label="CNPJ ou CPF" name="cnpjCpf" required>
          <UInput
            :model-value="state.cnpjCpf"
            placeholder="00.000.000/0001-00"
            icon="i-lucide-file-text"
            size="md"
            class="code-mono"
            @update:model-value="onDocInput"
          />
        </UFormGroup>

        <UFormGroup label="Setor de atuação" name="setorAtuacao" required>
          <USelectMenu
            v-model="state.setorAtuacao"
            :options="[...SETORES_ATUACAO]"
            placeholder="Selecione o setor"
            size="md"
          />
        </UFormGroup>
      </div>

      <div class="grid gap-3.5 sm:grid-cols-2">
        <UFormGroup label="Telefone" name="telefone" required>
          <UInput
            :model-value="state.telefone"
            placeholder="(11) 99999-9999"
            icon="i-lucide-phone"
            size="md"
            @update:model-value="onPhoneInput"
          />
        </UFormGroup>

        <UFormGroup label="E-mail corporativo" name="email" required>
          <UInput
            v-model="state.email"
            type="email"
            placeholder="contato@empresa.com.br"
            icon="i-lucide-mail"
            size="md"
            autocomplete="email"
          />
        </UFormGroup>
      </div>

      <UFormGroup label="Senha de acesso" name="senha" required>
        <UInput
          v-model="state.senha"
          :type="showPassword ? 'text' : 'password'"
          placeholder="Crie uma senha forte"
          icon="i-lucide-lock"
          size="md"
          autocomplete="new-password"
          :ui="{ icon: { trailing: { pointer: '' } } }"
        >
          <template #trailing>
            <UButton
              color="gray"
              variant="link"
              :icon="showPassword ? 'i-lucide-eye-off' : 'i-lucide-eye'"
              :padded="false"
              @click="showPassword = !showPassword"
            />
          </template>
        </UInput>
        <FormsPasswordStrengthMeter :password="state.senha" />
      </UFormGroup>

      <UFormGroup label="Confirmar senha" name="confirmarSenha" required>
        <UInput
          v-model="state.confirmarSenha"
          :type="showConfirmPassword ? 'text' : 'password'"
          placeholder="Repita a senha informada"
          icon="i-lucide-shield-check"
          size="md"
          autocomplete="new-password"
          :ui="{ icon: { trailing: { pointer: '' } } }"
        >
          <template #trailing>
            <UButton
              color="gray"
              variant="link"
              :icon="showConfirmPassword ? 'i-lucide-eye-off' : 'i-lucide-eye'"
              :padded="false"
              @click="showConfirmPassword = !showConfirmPassword"
            />
          </template>
        </UInput>
      </UFormGroup>

      <UButton
        type="submit"
        color="blue"
        size="lg"
        block
        :loading="authStore.loading"
        icon="i-lucide-user-plus"
        class="mt-2"
      >
        Finalizar Cadastro
      </UButton>
    </UForm>

    <p class="mt-5 text-center text-sm text-slate-600">
      Já possui cadastro?
      <NuxtLink to="/auth/login" class="font-semibold text-blue-600 hover:text-blue-700 hover:underline">
        Voltar para o login
      </NuxtLink>
    </p>
  </div>
</template>
