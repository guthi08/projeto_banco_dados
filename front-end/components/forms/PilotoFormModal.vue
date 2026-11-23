<script setup lang="ts">
import type { FormSubmitEvent } from '#ui/types'
import { formatPhoneBR } from '~/schemas/auth.schema'
import {
  formatCpf,
  formatLicenseCode,
  pilotoSchema,
  type PilotoFormValues
} from '~/schemas/piloto.schema'
import type { PilotoRecord } from '~/stores/pilotos'

const props = defineProps<{
  modelValue: boolean
  editingPiloto?: PilotoRecord | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [piloto: PilotoRecord]
}>()

const authStore = useAuthStore()
const pilotosStore = usePilotosStore()
const empresasStore = useEmpresasStore()
const toast = useToast()
const { getValidityStatus, daysUntil } = useDateHelper()

const errorMessage = ref<string | null>(null)

const state = reactive<PilotoFormValues>({
  cpf: '',
  rg: '',
  nome: '',
  email: '',
  numRegistroLicenca: '',
  validadeCma: '',
  telefones: [''],
  empresaOpeCnpj: ''
})

const isEditing = computed(() => Boolean(props.editingPiloto))

const isOpen = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val)
})

const empresaOptions = computed(() =>
  empresasStore.empresas.map((e) => ({
    label: `${e.razaoSocial} (${e.cnpj})`,
    value: e.cnpj
  }))
)

const cmaPreview = computed(() => {
  if (!state.validadeCma || !/^\d{4}-\d{2}-\d{2}$/.test(state.validadeCma)) {
    return null
  }
  const level = getValidityStatus(state.validadeCma, 30)
  const days = daysUntil(state.validadeCma)
  if (level === 'expired') {
    return {
      color: 'red' as const,
      icon: 'i-lucide-shield-x',
      title: 'CMA expirado — Trava de Segurança',
      description: 'Este piloto ficará bloqueado e desabilitado para novas escalas de voo até a renovação do CMA.'
    }
  }
  if (level === 'expiring') {
    return {
      color: 'amber' as const,
      icon: 'i-lucide-clock-alert',
      title: `CMA vence em ${days} dia(s)`,
      description: 'Programe a renovação do Certificado Médico Aeronáutico antes do vencimento.'
    }
  }
  return {
    color: 'green' as const,
    icon: 'i-lucide-shield-check',
    title: 'CMA regular',
    description: `Piloto apto para escalas de missão pelos próximos ${days} dias.`
  }
})

watch(
  () => [props.modelValue, props.editingPiloto] as const,
  ([open, piloto]) => {
    if (!open) return
    errorMessage.value = null
    if (piloto) {
      state.cpf = piloto.cpf
      state.rg = piloto.rg
      state.nome = piloto.nome
      state.email = piloto.email
      state.numRegistroLicenca = piloto.numRegistroLicenca
      state.validadeCma = piloto.validadeCma
      state.telefones = piloto.telefones.length ? [...piloto.telefones] : ['']
      state.empresaOpeCnpj = piloto.empresaOpeCnpj
    } else {
      state.cpf = ''
      state.rg = ''
      state.nome = ''
      state.email = ''
      state.numRegistroLicenca = 'ANAC-RPAS-'
      state.validadeCma = new Date(Date.now() + 365 * 86400000).toISOString().slice(0, 10)
      state.telefones = ['']
      state.empresaOpeCnpj = authStore.isAdmin
        ? (empresasStore.activeOperator?.cnpj || empresasStore.empresas[0]?.cnpj || '')
        : (authStore.user?.empresaCnpj || empresasStore.activeOperator?.cnpj || '')
    }
  }
)

function onCpfUpdate(value: string) {
  state.cpf = formatCpf(value)
}

function onLicenseUpdate(value: string) {
  state.numRegistroLicenca = formatLicenseCode(value)
}

function onPhoneUpdate(index: number, value: string) {
  state.telefones[index] = formatPhoneBR(value)
}

function addPhoneField() {
  if (state.telefones.length >= 4) return
  state.telefones.push('')
}

function removePhoneField(index: number) {
  if (state.telefones.length <= 1) return
  state.telefones.splice(index, 1)
}

async function onSubmit(event: FormSubmitEvent<PilotoFormValues>) {
  errorMessage.value = null
  try {
    let saved: PilotoRecord
    if (isEditing.value && props.editingPiloto) {
      saved = await pilotosStore.updatePiloto(props.editingPiloto.cpf, event.data)
      toast.add({
        title: 'Piloto atualizado',
        description: `Dados e CMA de ${saved.nome} salvos com sucesso.`,
        icon: 'i-lucide-check-circle-2',
        color: 'green'
      })
    } else {
      saved = await pilotosStore.createPiloto(event.data)
      toast.add({
        title: 'Piloto cadastrado',
        description: `${saved.nome} (${saved.numRegistroLicenca}) adicionado ao quadro de pilotos.`,
        icon: 'i-lucide-user-check',
        color: 'green'
      })
    }

    emit('saved', saved)
    isOpen.value = false
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : extractApiErrorMessage(err)
  }
}
</script>

<template>
  <UModal v-model="isOpen" :ui="{ width: 'sm:max-w-xl' }">
    <UCard>
      <template #header>
        <div class="flex items-center justify-between gap-2">
          <div>
            <span class="code-mono text-[11px] font-semibold uppercase tracking-wider text-blue-600">
              Habilitação ANAC & CMA
            </span>
            <h3 class="text-base font-semibold text-slate-900">
              {{ isEditing ? `Editar Piloto — ${editingPiloto?.nome}` : 'Cadastrar Novo Piloto' }}
            </h3>
          </div>
          <UButton
            color="gray"
            variant="ghost"
            icon="i-lucide-x"
            size="sm"
            aria-label="Fechar modal"
            @click="isOpen = false"
          />
        </div>
      </template>

      <UAlert
        v-if="errorMessage"
        color="red"
        variant="soft"
        icon="i-lucide-alert-circle"
        :description="errorMessage"
        class="mb-4"
      />

      <UForm :schema="pilotoSchema" :state="state" class="space-y-4" @submit="onSubmit">
        <UFormGroup
          label="Nome completo do piloto"
          description="Informe nome e sobrenome completos conforme registro na ANAC."
          name="nome"
          required
        >
          <UInput
            v-model="state.nome"
            placeholder="Ex: Carlos Eduardo Silva"
            icon="i-lucide-user"
          />
        </UFormGroup>

        <div class="grid gap-4 sm:grid-cols-2">
          <UFormGroup label="CPF" name="cpf" required>
            <UInput
              :model-value="state.cpf"
              :disabled="isEditing"
              placeholder="000.000.000-00"
              icon="i-lucide-id-card"
              class="code-mono"
              @update:model-value="onCpfUpdate"
            />
          </UFormGroup>

          <UFormGroup label="RG" name="rg" required>
            <UInput
              v-model="state.rg"
              placeholder="SP12345678"
              icon="i-lucide-file-text"
              class="code-mono"
            />
          </UFormGroup>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <UFormGroup label="Registro / Licença de Voo" name="numRegistroLicenca" required>
            <UInput
              :model-value="state.numRegistroLicenca"
              placeholder="ANAC-RPAS-88412"
              icon="i-lucide-award"
              class="code-mono"
              @update:model-value="onLicenseUpdate"
            />
          </UFormGroup>

          <UFormGroup label="Validade do CMA" name="validadeCma" required>
            <UInput
              v-model="state.validadeCma"
              type="date"
              class="code-mono"
            />
          </UFormGroup>
        </div>

        <div :class="authStore.isAdmin ? 'grid gap-4 sm:grid-cols-2' : ''">
          <UFormGroup label="E-mail corporativo" name="email" required>
            <UInput
              v-model="state.email"
              type="email"
              placeholder="piloto@operadora.com.br"
              icon="i-lucide-mail"
            />
          </UFormGroup>

          <UFormGroup
            v-if="authStore.isAdmin"
            label="Empresa operadora"
            name="empresaOpeCnpj"
            required
          >
            <USelect
              v-model="state.empresaOpeCnpj"
              :options="empresaOptions"
              option-attribute="label"
              value-attribute="value"
              placeholder="Selecione a operadora"
            />
          </UFormGroup>
        </div>

        <!-- Múltiplos Telefones (Tabela telefone_piloto) -->
        <UFormGroup label="Telefones de contato" name="telefones" required>
          <div class="space-y-2">
            <div
              v-for="(phone, idx) in state.telefones"
              :key="idx"
              class="flex items-center gap-2"
            >
              <UInput
                :model-value="phone"
                placeholder="(11) 99999-9999"
                icon="i-lucide-phone"
                class="flex-1"
                @update:model-value="(val) => onPhoneUpdate(idx, val)"
              />
              <UButton
                v-if="state.telefones.length > 1"
                color="red"
                variant="ghost"
                icon="i-lucide-trash-2"
                size="sm"
                aria-label="Remover telefone"
                @click="removePhoneField(idx)"
              />
            </div>

            <UButton
              v-if="state.telefones.length < 4"
              color="gray"
              variant="soft"
              size="xs"
              icon="i-lucide-plus"
              @click="addPhoneField"
            >
              Adicionar outro telefone
            </UButton>
          </div>
        </UFormGroup>

        <UAlert
          v-if="cmaPreview"
          :color="cmaPreview.color"
          variant="soft"
          :icon="cmaPreview.icon"
          :title="cmaPreview.title"
          :description="cmaPreview.description"
        />

        <div class="flex justify-end gap-2 border-t border-slate-200 pt-4">
          <UButton color="gray" variant="outline" @click="isOpen = false">
            Cancelar
          </UButton>
          <UButton
            type="submit"
            color="blue"
            icon="i-lucide-save"
            :loading="pilotosStore.loading"
          >
            {{ isEditing ? 'Salvar Alterações' : 'Cadastrar Piloto' }}
          </UButton>
        </div>
      </UForm>
    </UCard>
  </UModal>
</template>
