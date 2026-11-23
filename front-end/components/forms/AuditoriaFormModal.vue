<script setup lang="ts">
import dayjs from 'dayjs'
import type { FormSubmitEvent } from '#ui/types'
import {
  AUDITORIA_CATEGORIAS,
  GRAVIDADE_OPTIONS,
  auditoriaSchema,
  type AuditoriaFormValues
} from '~/schemas/auditoria.schema'
import type { AuditoriaRecord } from '~/stores/auditorias'

const props = defineProps<{
  modelValue: boolean
  lockedOperacaoCodigo?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [auditoria: AuditoriaRecord]
}>()

const authStore = useAuthStore()
const auditoriasStore = useAuditoriasStore()
const operacoesStore = useOperacoesStore()
const empresasStore = useEmpresasStore()
const toast = useToast()

const errorMessage = ref<string | null>(null)
const selectedEmpresaCnpj = ref<string>('')

const onlyDigits = (val: string) => val.replace(/\D/g, '')

const state = reactive<AuditoriaFormValues>({
  operacaoVooCodigo: 0,
  dataHoraEvento: '',
  categoria: 'Desvio de Rota',
  nivelGravidade: 'MEDIO',
  descricao: ''
})

const isOpen = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val)
})

const showEmpresaSelector = computed(() => authStore.isAdmin && !props.lockedOperacaoCodigo)

const empresaOptions = computed(() =>
  empresasStore.empresas.map((e) => ({
    label: `${e.razaoSocial} (${e.cnpj})`,
    value: e.cnpj
  }))
)

const filteredOperacoes = computed(() => {
  if (!showEmpresaSelector.value || !selectedEmpresaCnpj.value) {
    return operacoesStore.operacoes
  }
  const target = onlyDigits(selectedEmpresaCnpj.value)
  return operacoesStore.operacoes.filter((op) => onlyDigits(op.empresaOpeCnpj) === target)
})

const operacaoOptions = computed(() =>
  filteredOperacoes.value.map((op) => {
    const resolved = operacoesStore.resolveLabels(op)
    return {
      label: `${op.displayCode} — ${op.droneCodigo} · ${resolved.pilotName} (${op.objetivoVoo.slice(0, 32)})`,
      value: op.codigo
    }
  })
)

watch(
  () => [props.modelValue, props.lockedOperacaoCodigo] as const,
  ([open, locked]) => {
    if (!open) return
    errorMessage.value = null

    if (showEmpresaSelector.value) {
      const defaultEmpresa =
        empresasStore.activeOperator?.cnpj ||
        empresasStore.empresas[0]?.cnpj ||
        ''
      selectedEmpresaCnpj.value = defaultEmpresa
      const firstOp = operacoesStore.operacoes.find(
        (op) => !defaultEmpresa || onlyDigits(op.empresaOpeCnpj) === onlyDigits(defaultEmpresa)
      )
      state.operacaoVooCodigo = firstOp?.codigo || 0
    } else {
      selectedEmpresaCnpj.value = authStore.user?.empresaCnpj || ''
      state.operacaoVooCodigo = locked || operacoesStore.operacoes[0]?.codigo || 0
    }

    state.dataHoraEvento = dayjs().format('YYYY-MM-DDTHH:mm')
    state.categoria = 'Desvio de Rota'
    state.nivelGravidade = 'MEDIO'
    state.descricao = ''
  }
)

watch(selectedEmpresaCnpj, () => {
  if (!showEmpresaSelector.value || !props.modelValue) return
  const existsInList = filteredOperacoes.value.some((op) => op.codigo === state.operacaoVooCodigo)
  if (!existsInList) {
    state.operacaoVooCodigo = filteredOperacoes.value[0]?.codigo || 0
  }
})

async function onSubmit(event: FormSubmitEvent<AuditoriaFormValues>) {
  errorMessage.value = null
  try {
    const created = await auditoriasStore.createAuditoria(event.data)
    toast.add({
      title: 'Auditoria registrada',
      description: `Ocorrência ${created.displayId} vinculada ao voo ${created.operacaoDisplayCode}.`,
      icon: 'i-lucide-shield-alert',
      color: created.nivelGravidade === 'CRITICO' || created.nivelGravidade === 'ALTO' ? 'amber' : 'green'
    })
    emit('saved', created)
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
              Registro Técnico Imutável
            </span>
            <h3 class="text-base font-semibold text-slate-900">
              Lançar Ocorrência / Auditoria de Voo
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

      <UAlert
        color="blue"
        variant="soft"
        icon="i-lucide-lock"
        title="Conformidade ANAC"
        description="Registros de auditoria são imutáveis após a gravação e ficam vinculados estritamente à operação de voo selecionada."
        class="mb-4"
      />

      <UForm :schema="auditoriaSchema" :state="state" class="space-y-4" @submit="onSubmit">
        <UFormGroup
          v-if="showEmpresaSelector"
          label="Empresa operadora"
          description="Selecione a empresa para listar as operações de voo vinculadas."
          required
        >
          <USelect
            v-model="selectedEmpresaCnpj"
            :options="empresaOptions"
            option-attribute="label"
            value-attribute="value"
            placeholder="Selecione a empresa operadora"
          />
        </UFormGroup>

        <UFormGroup label="Operação de voo vinculada" name="operacaoVooCodigo" required>
          <USelect
            v-model="state.operacaoVooCodigo"
            :options="operacaoOptions"
            option-attribute="label"
            value-attribute="value"
            :disabled="Boolean(lockedOperacaoCodigo) || operacaoOptions.length === 0"
            :placeholder="operacaoOptions.length === 0 ? 'Nenhuma operação cadastrada para esta empresa' : 'Selecione a missão'"
          />
        </UFormGroup>

        <div class="grid gap-4 sm:grid-cols-2">
          <UFormGroup label="Categoria da ocorrência" name="categoria" required>
            <USelect
              v-model="state.categoria"
              :options="[...AUDITORIA_CATEGORIAS]"
            />
          </UFormGroup>

          <UFormGroup label="Nível de gravidade" name="nivelGravidade" required>
            <USelect
              v-model="state.nivelGravidade"
              :options="GRAVIDADE_OPTIONS"
              option-attribute="label"
              value-attribute="value"
            />
          </UFormGroup>
        </div>

        <UFormGroup label="Data e hora do evento" name="dataHoraEvento" required>
          <UInput
            v-model="state.dataHoraEvento"
            type="datetime-local"
            class="code-mono"
          />
        </UFormGroup>

        <UFormGroup label="Descrição detalhada da ocorrência" name="descricao" required>
          <UTextarea
            v-model="state.descricao"
            :rows="4"
            placeholder="Relate o evento técnico, condições ambientais, desvios de rota ou medidas corretivas adotadas..."
          />
        </UFormGroup>

        <div class="flex justify-end gap-2 border-t border-slate-200 pt-4">
          <UButton color="gray" variant="outline" @click="isOpen = false">
            Cancelar
          </UButton>
          <UButton
            type="submit"
            color="blue"
            icon="i-lucide-shield-plus"
            :loading="auditoriasStore.loading"
            :disabled="operacaoOptions.length === 0"
          >
            Registrar Auditoria
          </UButton>
        </div>
      </UForm>
    </UCard>
  </UModal>
</template>
