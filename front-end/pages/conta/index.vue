<script setup lang="ts">
import type { FormSubmitEvent } from '#ui/types'
import {
  SETORES_ATUACAO,
  changePasswordSchema,
  formatPhoneBR,
  profileSchema,
  type ChangePasswordFormValues,
  type ProfileFormValues
} from '~/schemas/auth.schema'

definePageMeta({
  layout: 'default',
  middleware: 'auth'
})

useHead({
  title: 'Perfil e Segurança da Conta — DRONE-MONITOR'
})

const authStore = useAuthStore()
const toast = useToast()
const { formatDateBR } = useDateHelper()

const deactivateModalOpen = ref(false)
const deactivatePassword = ref('')
const deactivateError = ref<string | null>(null)
const showProfilePass = ref(false)
const showDeactivatePass = ref(false)
const profileError = ref<string | null>(null)
const passwordError = ref<string | null>(null)
const showCurrentPass = ref(false)
const showNewPass = ref(false)

const profileState = reactive<ProfileFormValues>({
  nome: authStore.user?.nome || '',
  razaoSocial: authStore.user?.razaoSocial || '',
  setorAtuacao: authStore.user?.setorAtuacao || '',
  telefone: authStore.user?.telefone || '',
  email: authStore.user?.email || '',
  senhaAtual: ''
})

const passwordState = reactive<ChangePasswordFormValues>({
  senhaAtual: '',
  novaSenha: '',
  confirmarNovaSenha: ''
})

watch(
  () => authStore.user,
  (u) => {
    if (!u) return
    profileState.nome = u.nome
    profileState.razaoSocial = u.razaoSocial
    profileState.setorAtuacao = u.setorAtuacao
    profileState.telefone = u.telefone
    profileState.email = u.email
  }
)

function onPhoneUpdate(value: string) {
  profileState.telefone = formatPhoneBR(value)
}

async function onProfileSubmit(event: FormSubmitEvent<ProfileFormValues>) {
  profileError.value = null
  try {
    await authStore.updateProfile(event.data)
    profileState.senhaAtual = ''
    toast.add({
      title: 'Perfil atualizado',
      description: 'Os dados cadastrais do operador foram salvos com sucesso.',
      icon: 'i-lucide-check-circle-2',
      color: 'green'
    })
  } catch (err) {
    profileError.value = extractApiErrorMessage(err, 'Não foi possível salvar as alterações de perfil.')
  }
}

async function onPasswordSubmit(event: FormSubmitEvent<ChangePasswordFormValues>) {
  passwordError.value = null
  try {
    const res = await authStore.changePassword({
      senhaAtual: event.data.senhaAtual,
      novaSenha: event.data.novaSenha
    })

    passwordState.senhaAtual = ''
    passwordState.novaSenha = ''
    passwordState.confirmarNovaSenha = ''

    toast.add({
      title: 'Senha alterada',
      description: res.message || 'Sua senha de acesso foi atualizada.',
      icon: 'i-lucide-shield-check',
      color: 'green'
    })
  } catch (err) {
    passwordError.value = extractApiErrorMessage(err, 'Não foi possível atualizar a senha.')
  }
}

function openDeactivateModal() {
  deactivatePassword.value = ''
  deactivateError.value = null
  showDeactivatePass.value = false
  deactivateModalOpen.value = true
}

async function confirmDeactivateAccount() {
  deactivateError.value = null
  if (!deactivatePassword.value.trim()) {
    deactivateError.value = 'Informe sua senha atual para confirmar a exclusão da conta.'
    return
  }
  try {
    await authStore.deactivateAccount(deactivatePassword.value)
    deactivateModalOpen.value = false
    deactivatePassword.value = ''
    toast.add({
      title: 'Conta excluída com sucesso',
      description: 'Desativação concluída e sessão encerrada.',
      icon: 'i-lucide-user-x',
      color: 'amber'
    })
  } catch (err) {
    deactivateError.value = extractApiErrorMessage(err, 'Senha atual incorreta ou erro ao excluir conta.')
  }
}
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-6">
    <!-- Cabeçalho da View -->
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold text-slate-900 sm:text-2xl">
          Perfil e Segurança da Conta
        </h1>
        <p class="mt-1 text-sm text-slate-500">
          Gerencie os dados cadastrais da empresa operadora, credenciais JWT e status da conta.
        </p>
      </div>

      <UBadge
        :color="authStore.user?.statusContrato === 'Ativo' ? 'green' : 'amber'"
        variant="subtle"
        size="md"
        class="code-mono"
      >
        CONTRATO {{ authStore.user?.statusContrato?.toUpperCase() || 'ATIVO' }}
      </UBadge>
    </div>

    <!-- Card Resumo da Sessão -->
    <div class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div class="flex items-center gap-4">
          <span class="inline-flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-sidebar-bg text-lg font-bold text-white">
            {{ authStore.userInitials }}
          </span>
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="text-base font-semibold text-slate-900">
                {{ authStore.user?.nome }}
              </h2>
              <UBadge color="blue" variant="subtle" size="xs" class="code-mono">
                {{ authStore.user?.role || 'OPERADOR' }}
              </UBadge>
            </div>
            <p class="text-sm text-slate-600">
              {{ authStore.user?.razaoSocial }}
            </p>
            <div class="code-mono mt-1 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
              <span>CNPJ/CPF: {{ authStore.user?.empresaCnpj || '—' }}</span>
              <span>Cadastro: {{ formatDateBR(authStore.user?.criadoEm) }}</span>
            </div>
          </div>
        </div>

        <UButton
          color="gray"
          variant="outline"
          icon="i-lucide-log-out"
          @click="authStore.logout({ redirect: true })"
        >
          Encerrar Sessão
        </UButton>
      </div>
    </div>

    <!-- Grid: Dados Cadastrais + Alterar Senha -->
    <div class="grid gap-6 lg:grid-cols-12">
      <!-- Formulário de Perfil -->
      <section class="rounded-xl border border-slate-200 bg-white shadow-sm lg:col-span-7">
        <div class="border-b border-slate-200 px-6 py-4">
          <h3 class="text-base font-semibold text-slate-900">
            Dados Cadastrais do Operador
          </h3>
          <p class="text-xs text-slate-500">
            Informações sincronizadas com o cadastro da empresa operadora.
          </p>
        </div>

        <div class="p-6">
          <UAlert
            v-if="profileError"
            color="red"
            variant="soft"
            icon="i-lucide-alert-circle"
            :description="profileError"
            class="mb-4"
          />

          <UForm :schema="profileSchema" :state="profileState" class="space-y-4" @submit="onProfileSubmit">
            <UFormGroup label="Nome do responsável" name="nome" required>
              <UInput v-model="profileState.nome" icon="i-lucide-user" size="md" />
            </UFormGroup>

            <UFormGroup label="Razão social" name="razaoSocial" required>
              <UInput v-model="profileState.razaoSocial" icon="i-lucide-building-2" size="md" />
            </UFormGroup>

            <div class="grid gap-4 sm:grid-cols-2">
              <UFormGroup label="Setor de atuação" name="setorAtuacao" required>
                <USelectMenu
                  v-model="profileState.setorAtuacao"
                  :options="[...SETORES_ATUACAO]"
                  size="md"
                />
              </UFormGroup>

              <UFormGroup label="Telefone" name="telefone" required>
                <UInput
                  :model-value="profileState.telefone"
                  icon="i-lucide-phone"
                  size="md"
                  @update:model-value="onPhoneUpdate"
                />
              </UFormGroup>
            </div>

            <UFormGroup label="E-mail corporativo" name="email" required>
              <UInput v-model="profileState.email" type="email" icon="i-lucide-mail" size="md" />
            </UFormGroup>

            <UFormGroup
              label="Senha atual (confirmação)"
              description="Informe sua senha atual para confirmar a edição do perfil."
              name="senhaAtual"
              required
            >
              <UInput
                v-model="profileState.senhaAtual"
                :type="showProfilePass ? 'text' : 'password'"
                placeholder="Digite sua senha atual"
                icon="i-lucide-lock"
                size="md"
                autocomplete="current-password"
                :ui="{ icon: { trailing: { pointer: '' } } }"
              >
                <template #trailing>
                  <UButton
                    color="gray"
                    variant="link"
                    :icon="showProfilePass ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                    :padded="false"
                    @click="showProfilePass = !showProfilePass"
                  />
                </template>
              </UInput>
            </UFormGroup>

            <div class="flex justify-end pt-2">
              <UButton type="submit" color="blue" icon="i-lucide-save" :loading="authStore.loading">
                Salvar Alterações
              </UButton>
            </div>
          </UForm>
        </div>
      </section>

      <!-- Alteração de Senha -->
      <section class="rounded-xl border border-slate-200 bg-white shadow-sm lg:col-span-5">
        <div class="border-b border-slate-200 px-6 py-4">
          <h3 class="text-base font-semibold text-slate-900">
            Segurança e Senha
          </h3>
          <p class="text-xs text-slate-500">
            Utilize uma senha forte para proteger o acesso ao painel.
          </p>
        </div>

        <div class="p-6">
          <UAlert
            v-if="passwordError"
            color="red"
            variant="soft"
            icon="i-lucide-alert-circle"
            :description="passwordError"
            class="mb-4"
          />

          <UForm :schema="changePasswordSchema" :state="passwordState" class="space-y-4" @submit="onPasswordSubmit">
            <UFormGroup label="Senha atual" name="senhaAtual" required>
              <UInput
                v-model="passwordState.senhaAtual"
                :type="showCurrentPass ? 'text' : 'password'"
                icon="i-lucide-lock"
                autocomplete="current-password"
                :ui="{ icon: { trailing: { pointer: '' } } }"
              >
                <template #trailing>
                  <UButton
                    color="gray"
                    variant="link"
                    :icon="showCurrentPass ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                    :padded="false"
                    @click="showCurrentPass = !showCurrentPass"
                  />
                </template>
              </UInput>
            </UFormGroup>

            <UFormGroup label="Nova senha" name="novaSenha" required>
              <UInput
                v-model="passwordState.novaSenha"
                :type="showNewPass ? 'text' : 'password'"
                icon="i-lucide-key-round"
                autocomplete="new-password"
                :ui="{ icon: { trailing: { pointer: '' } } }"
              >
                <template #trailing>
                  <UButton
                    color="gray"
                    variant="link"
                    :icon="showNewPass ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                    :padded="false"
                    @click="showNewPass = !showNewPass"
                  />
                </template>
              </UInput>
              <FormsPasswordStrengthMeter :password="passwordState.novaSenha" />
            </UFormGroup>

            <UFormGroup label="Confirmar nova senha" name="confirmarNovaSenha" required>
              <UInput
                v-model="passwordState.confirmarNovaSenha"
                type="password"
                icon="i-lucide-shield-check"
                autocomplete="new-password"
              />
            </UFormGroup>

            <UButton type="submit" color="blue" block icon="i-lucide-lock-keyhole" :loading="authStore.loading">
              Atualizar Senha
            </UButton>
          </UForm>
        </div>
      </section>
    </div>

    <!-- Zona de Desativação Lógica (MOD-BE 1) -->
    <section class="rounded-xl border border-red-200 bg-white shadow-sm">
      <div class="flex flex-wrap items-center justify-between gap-4 p-5">
        <div class="flex items-start gap-3">
          <span class="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-red-50 text-red-600">
            <UIcon name="i-lucide-alert-triangle" class="h-5 w-5" />
          </span>
          <div>
            <h3 class="text-sm font-semibold text-slate-900">
              Desativação Lógica da Conta
            </h3>
            <p class="mt-0.5 text-xs text-slate-500">
              Suspende o acesso do operador preservando integralmente o histórico de voos e auditorias ANAC no banco de dados.
            </p>
          </div>
        </div>

        <UButton color="red" variant="soft" icon="i-lucide-trash-2" @click="openDeactivateModal">
          Excluir Conta
        </UButton>
      </div>
    </section>

    <!-- Modal de Confirmação -->
    <UModal v-model="deactivateModalOpen">
      <UCard>
        <template #header>
          <div class="flex items-center gap-2 text-red-700">
            <UIcon name="i-lucide-alert-octagon" class="h-5 w-5" />
            <h3 class="text-base font-semibold">
              Confirmar exclusão da conta
            </h3>
          </div>
        </template>

        <div class="space-y-4">
          <UAlert
            v-if="deactivateError"
            color="red"
            variant="soft"
            icon="i-lucide-alert-circle"
            :description="deactivateError"
          />

          <p class="text-sm leading-relaxed text-slate-600">
            Tem certeza de que deseja excluir/desativar a conta de
            <strong class="text-slate-900">{{ authStore.user?.email }}</strong>?
            Seu acesso será encerrado imediatamente. Para confirmar, informe sua senha atual:
          </p>

          <UFormGroup label="Senha atual" required>
            <UInput
              v-model="deactivatePassword"
              :type="showDeactivatePass ? 'text' : 'password'"
              placeholder="Digite sua senha atual"
              icon="i-lucide-lock"
              autocomplete="current-password"
              :ui="{ icon: { trailing: { pointer: '' } } }"
              @keyup.enter="confirmDeactivateAccount"
            >
              <template #trailing>
                <UButton
                  color="gray"
                  variant="link"
                  :icon="showDeactivatePass ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                  :padded="false"
                  @click="showDeactivatePass = !showDeactivatePass"
                />
              </template>
            </UInput>
          </UFormGroup>
        </div>

        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="gray" variant="outline" @click="deactivateModalOpen = false">
              Cancelar
            </UButton>
            <UButton color="red" icon="i-lucide-trash-2" :loading="authStore.loading" @click="confirmDeactivateAccount">
              Confirmar Exclusão
            </UButton>
          </div>
        </template>
      </UCard>
    </UModal>
  </div>
</template>
