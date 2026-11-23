<script setup lang="ts">
import type { FormSubmitEvent } from '#ui/types'
import { formatCpfCnpj, formatPhoneBR } from '~/schemas/auth.schema'
import {
  SETORES_ATUACAO,
  STATUS_CONTRATO_OPTIONS,
  empresaSchema,
  type EmpresaFormValues
} from '~/schemas/empresa.schema'
import type { EmpresaOperadoraRecord } from '~/stores/empresas'

const props = defineProps<{
  modelValue: boolean
  editingEmpresa?: EmpresaOperadoraRecord | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [empresa: EmpresaOperadoraRecord]
}>()

const empresasStore = useEmpresasStore()
const toast = useToast()

const errorMessage = ref<string | null>(null)

const state = reactive<EmpresaFormValues>({
  cnpj: '',
  razaoSocial: '',
  setorAtuacao: '',
  email: '',
  telefones: [''],
  statusContrato: 'Ativo'
})

const isEditing = computed(() => Boolean(props.editingEmpresa))

const isOpen = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val)
})

watch(
  () => [props.modelValue, props.editingEmpresa] as const,
  ([open, empresa]) => {
    if (!open) return
    errorMessage.value = null
    if (empresa) {
      state.cnpj = empresa.cnpj
      state.razaoSocial = empresa.razaoSocial
      state.setorAtuacao = empresa.setorAtuacao
      state.email = empresa.email
      state.telefones = empresa.telefones.length ? [...empresa.telefones] : ['']
      state.statusContrato = empresa.statusContrato
    } else {
      state.cnpj = ''
      state.razaoSocial = ''
      state.setorAtuacao = ''
      state.email = ''
      state.telefones = ['']
      state.statusContrato = 'Ativo'
    }
  }
)

function onDocUpdate(value: string) {
  state.cnpj = formatCpfCnpj(value)
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

async function onSubmit(event: FormSubmitEvent<EmpresaFormValues>) {
  errorMessage.value = null
  try {
    let saved: EmpresaOperadoraRecord

    if (isEditing.value && props.editingEmpresa) {
      saved = await empresasStore.updateEmpresa(props.editingEmpresa.cnpj, event.data)
      toast.add({
        title: 'Empresa atualizada',
        description: `${saved.razaoSocial} foi atualizada com sucesso.`,
        icon: 'i-lucide-check-circle-2',
        color: 'green'
      })
    } else {
      saved = await empresasStore.createEmpresa(event.data)
      toast.add({
        title: 'Empresa cadastrada',
        description: `${saved.razaoSocial} foi adicionada à rede de operadoras.`,
        icon: 'i-lucide-building-2',
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
              {{ isEditing ? 'Edição Cadastral' : 'Novo Credenciamento' }}
            </span>
            <h3 class="text-base font-semibold text-slate-900">
              {{ isEditing ? 'Editar Empresa Operadora' : 'Cadastrar Empresa Operadora' }}
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

      <UForm :schema="empresaSchema" :state="state" class="space-y-4" @submit="onSubmit">
        <div class="grid gap-4 sm:grid-cols-2">
          <UFormGroup label="CNPJ ou CPF" name="cnpj" required>
            <UInput
              :model-value="state.cnpj"
              :disabled="isEditing"
              placeholder="00.000.000/0001-00"
              icon="i-lucide-file-text"
              class="code-mono"
              @update:model-value="onDocUpdate"
            />
          </UFormGroup>

          <UFormGroup label="Status do contrato" name="statusContrato" required>
            <USelect
              v-model="state.statusContrato"
              :options="[...STATUS_CONTRATO_OPTIONS]"
            />
          </UFormGroup>
        </div>

        <UFormGroup
          label="Razão social ou nome completo do responsável"
          description="Informe a razão social completa ou, para CPF, nome e sobrenome do responsável."
          name="razaoSocial"
          required
        >
          <UInput
            v-model="state.razaoSocial"
            placeholder="Ex: AeroLog Brasil Ltda ou Carlos Eduardo Silva"
            icon="i-lucide-building-2"
          />
        </UFormGroup>

        <div class="grid gap-4 sm:grid-cols-2">
          <UFormGroup label="Setor de atuação" name="setorAtuacao" required>
            <USelectMenu
              v-model="state.setorAtuacao"
              :options="[...SETORES_ATUACAO]"
              placeholder="Selecione o setor"
            />
          </UFormGroup>

          <UFormGroup label="E-mail corporativo" name="email" required>
            <UInput
              v-model="state.email"
              type="email"
              placeholder="contato@operadora.com.br"
              icon="i-lucide-mail"
            />
          </UFormGroup>
        </div>

        <!-- Múltiplos Telefones (Tabela telefone_empresa) -->
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

        <div class="flex justify-end gap-2 border-t border-slate-200 pt-4">
          <UButton color="gray" variant="outline" @click="isOpen = false">
            Cancelar
          </UButton>
          <UButton
            type="submit"
            color="blue"
            icon="i-lucide-save"
            :loading="empresasStore.loading"
          >
            {{ isEditing ? 'Salvar Alterações' : 'Cadastrar Empresa' }}
          </UButton>
        </div>
      </UForm>
    </UCard>
  </UModal>
</template>
