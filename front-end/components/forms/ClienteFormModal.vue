<script setup lang="ts">
import type { FormSubmitEvent } from '#ui/types'
import { formatCpfCnpj, formatPhoneBR } from '~/schemas/auth.schema'
import {
  CLIENTE_STATUS_OPTIONS,
  clienteSchema,
  type ClienteFormValues
} from '~/schemas/cliente.schema'
import type { ClienteRecord } from '~/stores/clientes'

const props = defineProps<{
  modelValue: boolean
  editingCliente?: ClienteRecord | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [cliente: ClienteRecord]
}>()

const authStore = useAuthStore()
const clientesStore = useClientesStore()
const empresasStore = useEmpresasStore()
const toast = useToast()

const errorMessage = ref<string | null>(null)

const state = reactive<ClienteFormValues>({
  cpfRg: '',
  rg: '',
  nome: '',
  empresaOpeCnpj: '',
  statusContrato: 'Ativo',
  dataFormalizacao: '',
  demandaPrincipal: '',
  telefones: ['']
})

const isEditing = computed(() => Boolean(props.editingCliente))

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

watch(
  () => [props.modelValue, props.editingCliente] as const,
  ([open, cliente]) => {
    if (!open) return
    errorMessage.value = null
    if (cliente) {
      state.cpfRg = cliente.cpfRg
      state.rg = cliente.rg
      state.nome = cliente.nome
      state.empresaOpeCnpj = cliente.empresaOpeCnpj
      state.statusContrato = cliente.statusContrato === 'Inativo' ? 'Inativo' : 'Ativo'
      state.dataFormalizacao = cliente.dataFormalizacao
      state.demandaPrincipal = cliente.demandaPrincipal || ''
      state.telefones = cliente.telefones.length ? [...cliente.telefones] : ['']
    } else {
      state.cpfRg = ''
      state.rg = ''
      state.nome = ''
      state.empresaOpeCnpj = authStore.isAdmin
        ? (empresasStore.activeOperator?.cnpj || empresasStore.empresas[0]?.cnpj || '')
        : (authStore.user?.empresaCnpj || empresasStore.activeOperator?.cnpj || '')
      state.statusContrato = 'Ativo'
      state.dataFormalizacao = new Date().toISOString().slice(0, 10)
      state.demandaPrincipal = ''
      state.telefones = ['']
    }
  }
)

function onDocUpdate(value: string) {
  state.cpfRg = formatCpfCnpj(value)
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

async function onSubmit(event: FormSubmitEvent<ClienteFormValues>) {
  errorMessage.value = null
  try {
    let saved: ClienteRecord
    if (isEditing.value && props.editingCliente) {
      saved = await clientesStore.updateCliente(props.editingCliente.cpfRg, event.data)
      toast.add({
        title: 'Cliente atualizado',
        description: `Dados e contrato de ${saved.nome} salvos com sucesso.`,
        icon: 'i-lucide-check-circle-2',
        color: 'green'
      })
    } else {
      saved = await clientesStore.createCliente(event.data)
      toast.add({
        title: 'Cliente cadastrado',
        description: `${saved.nome} foi adicionado à carteira de clientes.`,
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
              Carteira & Relação Comercial
            </span>
            <h3 class="text-base font-semibold text-slate-900">
              {{ isEditing ? `Editar Cliente — ${editingCliente?.nome}` : 'Cadastrar Novo Cliente' }}
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

      <UForm :schema="clienteSchema" :state="state" class="space-y-4" @submit="onSubmit">
        <UFormGroup
          label="Nome completo ou razão social do cliente"
          description="Informe nome e sobrenome completos (pessoa física) ou razão social completa."
          name="nome"
          required
        >
          <UInput
            v-model="state.nome"
            placeholder="Ex: Fazenda Boa Vista ou João Carlos Pereira"
            icon="i-lucide-building-2"
          />
        </UFormGroup>

        <div class="grid gap-4 sm:grid-cols-2">
          <UFormGroup label="CPF ou CNPJ" name="cpfRg" required>
            <UInput
              :model-value="state.cpfRg"
              :disabled="isEditing"
              placeholder="00.000.000/0001-00"
              icon="i-lucide-file-text"
              class="code-mono"
              @update:model-value="onDocUpdate"
            />
          </UFormGroup>

          <UFormGroup label="RG / Inscrição" name="rg" required>
            <UInput
              v-model="state.rg"
              placeholder="MG12345678"
              icon="i-lucide-id-card"
              class="code-mono"
            />
          </UFormGroup>
        </div>

        <UFormGroup
          v-if="authStore.isAdmin"
          label="Empresa operadora contratada"
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

        <div class="grid gap-4 sm:grid-cols-2">
          <UFormGroup label="Status do contrato" name="statusContrato" required>
            <USelect
              v-model="state.statusContrato"
              :options="[...CLIENTE_STATUS_OPTIONS]"
            />
          </UFormGroup>

          <UFormGroup label="Data de formalização" name="dataFormalizacao" required>
            <UInput
              v-model="state.dataFormalizacao"
              type="date"
              class="code-mono"
            />
          </UFormGroup>
        </div>

        <UFormGroup label="Operação / Demanda principal" name="demandaPrincipal">
          <UInput
            v-model="state.demandaPrincipal"
            placeholder="Ex: Pulverização e Monitoramento Agrícola"
            icon="i-lucide-compass"
          />
        </UFormGroup>

        <!-- Múltiplos Telefones (Tabela telefone_cliente) -->
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
            :loading="clientesStore.loading"
          >
            {{ isEditing ? 'Salvar Alterações' : 'Cadastrar Cliente' }}
          </UButton>
        </div>
      </UForm>
    </UCard>
  </UModal>
</template>
