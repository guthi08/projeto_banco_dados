<script setup lang="ts">
import type { FormSubmitEvent } from '#ui/types'
import {
  FABRICANTES_SUGERIDOS,
  TIPO_USO_OPTIONS,
  droneSchema,
  formatAnacCode,
  type DroneFormValues
} from '~/schemas/drone.schema'
import type { DroneRecord } from '~/stores/drones'

const props = defineProps<{
  modelValue: boolean
  editingDrone?: DroneRecord | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [drone: DroneRecord]
}>()

const authStore = useAuthStore()
const dronesStore = useDronesStore()
const empresasStore = useEmpresasStore()
const toast = useToast()
const { getValidityStatus, daysUntil } = useDateHelper()

const errorMessage = ref<string | null>(null)

const state = reactive<DroneFormValues>({
  codigo: '',
  fabricante: '',
  modelo: '',
  numSerie: '',
  pesoMaxDecolagem: 2.5,
  tipoUso: 'basico',
  dataValidadeCadastro: '',
  empresaOpeCnpj: ''
})

const isEditing = computed(() => Boolean(props.editingDrone))

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

const validityPreview = computed(() => {
  if (!state.dataValidadeCadastro || !/^\d{4}-\d{2}-\d{2}$/.test(state.dataValidadeCadastro)) {
    return null
  }
  const level = getValidityStatus(state.dataValidadeCadastro, 30)
  const days = daysUntil(state.dataValidadeCadastro)
  if (level === 'expired') {
    return {
      color: 'red' as const,
      icon: 'i-lucide-shield-x',
      title: 'Cadastro ANAC expirado',
      description: 'Esta aeronave ficará bloqueada para novos agendamentos de voo até a renovação do registro.'
    }
  }
  if (level === 'expiring') {
    return {
      color: 'amber' as const,
      icon: 'i-lucide-clock-alert',
      title: `Vence em ${days} dia(s)`,
      description: 'O cadastro ANAC está próximo do vencimento (< 30 dias).'
    }
  }
  return {
    color: 'green' as const,
    icon: 'i-lucide-shield-check',
    title: 'Cadastro ANAC regular',
    description: `Aeronave apta para despacho pelos próximos ${days} dias.`
  }
})

watch(
  () => [props.modelValue, props.editingDrone] as const,
  ([open, drone]) => {
    if (!open) return
    errorMessage.value = null
    if (drone) {
      state.codigo = drone.codigo
      state.fabricante = drone.fabricante
      state.modelo = drone.modelo
      state.numSerie = drone.numSerie
      state.pesoMaxDecolagem = drone.pesoMaxDecolagem
      state.tipoUso = drone.tipoUso
      state.dataValidadeCadastro = drone.dataValidadeCadastro
      state.empresaOpeCnpj = drone.empresaOpeCnpj
    } else {
      state.codigo = 'PR-'
      state.fabricante = 'DJI'
      state.modelo = ''
      state.numSerie = ''
      state.pesoMaxDecolagem = 2.5
      state.tipoUso = 'basico'
      state.dataValidadeCadastro = new Date(Date.now() + 365 * 86400000).toISOString().slice(0, 10)
      state.empresaOpeCnpj = authStore.isAdmin
        ? (empresasStore.activeOperator?.cnpj || empresasStore.empresas[0]?.cnpj || '')
        : (authStore.user?.empresaCnpj || empresasStore.activeOperator?.cnpj || '')
    }
  }
)

function onCodeUpdate(value: string) {
  state.codigo = formatAnacCode(value)
}

async function onSubmit(event: FormSubmitEvent<DroneFormValues>) {
  errorMessage.value = null
  try {
    let saved: DroneRecord
    if (isEditing.value && props.editingDrone) {
      saved = await dronesStore.updateDrone(props.editingDrone.codigo, event.data)
      toast.add({
        title: 'Aeronave atualizada',
        description: `Drone ${saved.codigo} (${saved.modelo}) atualizado com sucesso.`,
        icon: 'i-lucide-check-circle-2',
        color: 'green'
      })
    } else {
      saved = await dronesStore.createDrone(event.data)
      toast.add({
        title: 'Drone cadastrado na frota',
        description: `Aeronave ${saved.codigo} vinculada à operadora.`,
        icon: 'i-lucide-cpu',
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
              Registro SISANT / ANAC
            </span>
            <h3 class="text-base font-semibold text-slate-900">
              {{ isEditing ? `Editar Aeronave ${editingDrone?.codigo}` : 'Cadastrar Novo Drone' }}
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

      <UForm :schema="droneSchema" :state="state" class="space-y-4" @submit="onSubmit">
        <div class="grid gap-4 sm:grid-cols-2">
          <UFormGroup label="Prefixo ANAC (PR / PP / PS)" name="codigo" required>
            <UInput
              :model-value="state.codigo"
              :disabled="isEditing"
              placeholder="PR-401"
              icon="i-lucide-hash"
              class="code-mono"
              @update:model-value="onCodeUpdate"
            />
          </UFormGroup>

          <UFormGroup label="Número de série" name="numSerie" required>
            <UInput
              v-model="state.numSerie"
              placeholder="SN-M300-88421"
              icon="i-lucide-barcode"
              class="code-mono"
            />
          </UFormGroup>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <UFormGroup label="Fabricante" name="fabricante" required>
            <UInput
              v-model="state.fabricante"
              list="fabricantes-list"
              placeholder="Ex: DJI, XAG, Autel"
              icon="i-lucide-factory"
            />
            <datalist id="fabricantes-list">
              <option v-for="fab in FABRICANTES_SUGERIDOS" :key="fab" :value="fab" />
            </datalist>
          </UFormGroup>

          <UFormGroup label="Modelo da aeronave" name="modelo" required>
            <UInput
              v-model="state.modelo"
              placeholder="Ex: Matrice 300 RTK"
              icon="i-lucide-cpu"
            />
          </UFormGroup>
        </div>

        <div class="grid gap-4 sm:grid-cols-3">
          <UFormGroup label="MTOW (kg)" name="pesoMaxDecolagem" required>
            <UInput
              v-model="state.pesoMaxDecolagem"
              type="number"
              step="0.01"
              min="0.1"
              max="999.99"
              placeholder="9.00"
              class="code-mono"
            />
          </UFormGroup>

          <UFormGroup label="Tipo de uso" name="tipoUso" required>
            <USelect
              v-model="state.tipoUso"
              :options="TIPO_USO_OPTIONS"
              option-attribute="label"
              value-attribute="value"
            />
          </UFormGroup>

          <UFormGroup label="Validade ANAC" name="dataValidadeCadastro" required>
            <UInput
              v-model="state.dataValidadeCadastro"
              type="date"
              class="code-mono"
            />
          </UFormGroup>
        </div>

        <UFormGroup
          v-if="authStore.isAdmin"
          label="Empresa operadora vinculada"
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

        <UAlert
          v-if="validityPreview"
          :color="validityPreview.color"
          variant="soft"
          :icon="validityPreview.icon"
          :title="validityPreview.title"
          :description="validityPreview.description"
        />

        <div class="flex justify-end gap-2 border-t border-slate-200 pt-4">
          <UButton color="gray" variant="outline" @click="isOpen = false">
            Cancelar
          </UButton>
          <UButton
            type="submit"
            color="blue"
            icon="i-lucide-save"
            :loading="dronesStore.loading"
          >
            {{ isEditing ? 'Salvar Alterações' : 'Cadastrar Drone' }}
          </UButton>
        </div>
      </UForm>
    </UCard>
  </UModal>
</template>
