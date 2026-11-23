<script setup lang="ts">
const route = useRoute()
const authStore = useAuthStore()
const filtersStore = useFiltersStore()
const empresasStore = useEmpresasStore()
const clientesStore = useClientesStore()
const toast = useToast()
const { operations, drones, pilots, audits, validityAlerts, refresh } = useDashboardData()

onMounted(() => {
  if (authStore.isAuthenticated) {
    refresh(false)
  }
})

const mobileSidebarOpen = ref(false)
const userMenuOpen = ref(false)
const topUserMenuOpen = ref(false)
const notifMenuOpen = ref(false)
const deleteAccountModalOpen = ref(false)
const deleteAccountPassword = ref('')
const showDeletePassword = ref(false)
const deleteAccountError = ref<string | null>(null)

const activeOperatorLabel = computed(
  () => empresasStore.activeOperator?.razaoSocial || authStore.operatorName
)

const navItems = computed(() => {
  const items = [
    { label: 'Dashboard', to: '/', icon: 'i-lucide-layout-dashboard', count: null as number | null },
    { label: 'Operações de Voo', to: '/operacoes', icon: 'i-lucide-navigation', count: operations.value.length },
    { label: 'Drones', to: '/drones', icon: 'i-lucide-cpu', count: drones.value.length },
    { label: 'Pilotos', to: '/pilotos', icon: 'i-lucide-user-check', count: pilots.value.length },
    { label: 'Clientes', to: '/clientes', icon: 'i-lucide-building-2', count: clientesStore.clientes.length },
    { label: 'Auditoria', to: '/auditorias', icon: 'i-lucide-shield-alert', count: audits.value.length }
  ]

  if (authStore.isAdmin) {
    items.splice(5, 0, {
      label: 'Empresas',
      to: '/empresas',
      icon: 'i-lucide-briefcase',
      count: empresasStore.empresas.length
    })
  }

  return items
})

const isActiveRoute = (path: string) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

watch(
  () => route.fullPath,
  () => {
    mobileSidebarOpen.value = false
    userMenuOpen.value = false
    topUserMenuOpen.value = false
    notifMenuOpen.value = false
  }
)

function handleLogout() {
  userMenuOpen.value = false
  topUserMenuOpen.value = false
  authStore.logout({ redirect: true })
  toast.add({
    title: 'Sessão encerrada',
    description: 'Você saiu do painel DRONE-MONITOR com segurança.',
    icon: 'i-lucide-log-out',
    color: 'blue'
  })
}

function openDeleteAccountModal() {
  topUserMenuOpen.value = false
  userMenuOpen.value = false
  deleteAccountPassword.value = ''
  deleteAccountError.value = null
  showDeletePassword.value = false
  deleteAccountModalOpen.value = true
}

async function confirmDeleteAccount() {
  deleteAccountError.value = null
  if (!deleteAccountPassword.value.trim()) {
    deleteAccountError.value = 'Informe sua senha atual para confirmar a exclusão da conta.'
    return
  }
  try {
    await authStore.deactivateAccount(deleteAccountPassword.value)
    deleteAccountModalOpen.value = false
    deleteAccountPassword.value = ''
    toast.add({
      title: 'Conta excluída com sucesso',
      description: 'Sua conta foi desativada e a sessão foi encerrada.',
      icon: 'i-lucide-user-x',
      color: 'amber'
    })
  } catch (err) {
    deleteAccountError.value = extractApiErrorMessage(err, 'Senha atual incorreta ou erro ao excluir conta.')
  }
}
</script>

<template>
  <div class="grid min-h-screen bg-canvas lg:grid-cols-[248px_minmax(0,1fr)]">
    <!-- Backdrop Mobile -->
    <div
      v-if="mobileSidebarOpen"
      class="fixed inset-0 z-30 bg-slate-900/50 lg:hidden"
      @click="mobileSidebarOpen = false"
    />

    <!-- Sidebar Dark Navy-Slate (Referência AERO-MONITOR) -->
    <aside
      class="fixed inset-y-0 left-0 z-40 flex w-[248px] flex-col justify-between border-r border-sidebar-border bg-sidebar-bg text-sidebar-ink transition-transform duration-200 lg:static lg:translate-x-0"
      :class="mobileSidebarOpen ? 'translate-x-0' : '-translate-x-full'"
      aria-label="Navegação principal"
    >
      <div>
        <div class="flex items-center justify-between border-b border-sidebar-border px-5 py-5">
          <NuxtLink to="/" class="focus:outline-none">
            <CommonBrandLogo theme="dark" />
          </NuxtLink>
          <button
            type="button"
            class="inline-flex h-8 w-8 items-center justify-center rounded-md text-slate-400 hover:bg-sidebar-hover hover:text-white lg:hidden"
            aria-label="Fechar menu"
            @click="mobileSidebarOpen = false"
          >
            <UIcon name="i-lucide-x" class="h-5 w-5" />
          </button>
        </div>

        <nav class="mt-4 px-2.5" aria-label="Módulos do sistema">
          <ul class="space-y-1">
            <li v-for="item in navItems" :key="item.to">
              <NuxtLink
                :to="item.to"
                class="flex min-h-[42px] items-center gap-2.5 rounded-md border-l-[3px] px-3 py-2 text-sm font-medium transition-colors"
                :class="
                  isActiveRoute(item.to)
                    ? 'border-blue-600 bg-sidebar-active font-semibold text-white'
                    : 'border-transparent text-slate-400 hover:bg-sidebar-hover hover:text-slate-100'
                "
              >
                <UIcon :name="item.icon" class="h-[18px] w-[18px] shrink-0" />
                <span class="truncate">{{ item.label }}</span>
                <span
                  v-if="item.count !== null"
                  class="code-mono ml-auto rounded-full px-2 py-0.5 text-[11px]"
                  :class="
                    isActiveRoute(item.to)
                      ? 'bg-blue-600 text-white'
                      : 'bg-sidebar-surface text-slate-400'
                  "
                >
                  {{ item.count }}
                </span>
              </NuxtLink>
            </li>
          </ul>
        </nav>
      </div>

      <!-- Rodapé do Usuário na Sidebar -->
      <div class="relative border-t border-sidebar-border p-3">
        <div
          v-if="userMenuOpen"
          class="absolute bottom-[calc(100%+6px)] left-3 right-3 z-50 rounded-lg border border-slate-200 bg-white p-1.5 text-slate-900 shadow-xl"
        >
          <NuxtLink
            to="/conta"
            class="flex w-full items-center gap-2.5 rounded-md px-2.5 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
          >
            <UIcon name="i-lucide-user-cog" class="h-4 w-4 text-slate-500" />
            <span>Perfil e Segurança</span>
          </NuxtLink>
          <NuxtLink
            v-if="authStore.isAdmin"
            to="/empresas"
            class="flex w-full items-center gap-2.5 rounded-md px-2.5 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
          >
            <UIcon name="i-lucide-briefcase" class="h-4 w-4 text-slate-500" />
            <span>Empresas Operadoras</span>
          </NuxtLink>
          <div class="my-1 h-px bg-slate-200" />
          <button
            type="button"
            class="flex w-full items-center gap-2.5 rounded-md px-2.5 py-2 text-left text-sm font-medium text-red-600 hover:bg-red-50"
            @click="handleLogout"
          >
            <UIcon name="i-lucide-log-out" class="h-4 w-4" />
            <span>Sair da Conta</span>
          </button>
        </div>

        <button
          type="button"
          class="flex w-full items-center gap-2.5 rounded-lg border border-sidebar-border bg-sidebar-surface p-2 text-left transition-colors hover:bg-sidebar-hover"
          :aria-expanded="userMenuOpen"
          @click="userMenuOpen = !userMenuOpen"
        >
          <span class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-600 text-xs font-semibold text-white">
            {{ authStore.userInitials }}
          </span>
          <span class="min-w-0 flex-1">
            <span class="block truncate text-sm font-semibold text-slate-50">
              {{ authStore.user?.nome || 'Operador' }}
            </span>
            <span class="block truncate text-xs text-slate-400">
              {{ activeOperatorLabel }}
            </span>
          </span>
          <UIcon name="i-lucide-chevrons-up-down" class="h-4 w-4 shrink-0 text-slate-400" />
        </button>
      </div>
    </aside>

    <!-- Coluna Principal -->
    <div class="flex min-w-0 flex-col">
      <header class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 py-3 sm:px-6">
        <div class="flex min-w-0 flex-1 items-center gap-3 sm:max-w-lg">
          <button
            type="button"
            class="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100 lg:hidden"
            aria-label="Abrir menu lateral"
            @click="mobileSidebarOpen = true"
          >
            <UIcon name="i-lucide-menu" class="h-5 w-5" />
          </button>

          <div class="relative flex-1">
            <UIcon
              name="i-lucide-search"
              class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
            />
            <input
              v-model="filtersStore.globalSearch"
              type="search"
              placeholder="Buscar operações, drones, pilotos, clientes..."
              class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 pl-9 pr-3 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-100"
            >
          </div>
        </div>

        <div class="relative flex items-center gap-2.5">
          <UBadge
            v-if="authStore.isAdmin"
            color="indigo"
            variant="subtle"
            size="sm"
            class="code-mono hidden md:inline-flex"
          >
            ADMIN
          </UBadge>

          <!-- Botão de Notificações e Alertas ANAC/CMA -->
          <button
            type="button"
            class="relative inline-flex h-10 w-10 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-900"
            aria-label="Notificações e alertas de validade"
            :aria-expanded="notifMenuOpen"
            @click="() => { notifMenuOpen = !notifMenuOpen; topUserMenuOpen = false }"
          >
            <UIcon name="i-lucide-bell" class="h-[18px] w-[18px]" />
            <span
              v-if="validityAlerts.length > 0"
              class="code-mono absolute -right-1 -top-1 inline-flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-semibold text-white"
            >
              {{ validityAlerts.length }}
            </span>
          </button>

          <!-- Popover de Notificações -->
          <div
            v-if="notifMenuOpen"
            class="absolute right-0 top-[calc(100%+8px)] z-50 w-80 max-w-[calc(100vw-2rem)] rounded-xl border border-slate-200 bg-white p-4 shadow-xl"
          >
            <div class="mb-3 flex items-center justify-between">
              <strong class="text-sm font-semibold text-slate-900">Avisos Operacionais</strong>
              <button
                type="button"
                class="text-xs font-semibold text-blue-600 hover:underline"
                @click="notifMenuOpen = false"
              >
                Fechar
              </button>
            </div>

            <div v-if="validityAlerts.length === 0" class="py-4 text-center text-xs text-slate-500">
              Nenhum alerta de vencimento ANAC/CMA ativo.
            </div>

            <div v-else class="space-y-2">
              <NuxtLink
                v-for="alert in validityAlerts.slice(0, 4)"
                :key="alert.id"
                :to="alert.route"
                class="block rounded-lg border-l-4 p-2.5 text-left transition-colors"
                :class="
                  alert.level === 'critical'
                    ? 'border-red-500 bg-red-50 text-red-900 hover:bg-red-100'
                    : 'border-amber-500 bg-amber-50 text-amber-900 hover:bg-amber-100'
                "
              >
                <p class="text-xs font-semibold">{{ alert.title }}</p>
                <p class="mt-0.5 text-[11px] opacity-90">{{ alert.subtitle }}</p>
              </NuxtLink>
            </div>
          </div>

          <!-- Menu do Usuário no Canto Superior Direito -->
          <div class="relative">
            <button
              type="button"
              class="inline-flex min-h-[40px] items-center gap-2 rounded-full border border-slate-200 bg-slate-50 py-1 pl-1 pr-3 transition-colors hover:bg-slate-100"
              title="Opções da conta"
              :aria-expanded="topUserMenuOpen"
              @click="() => { topUserMenuOpen = !topUserMenuOpen; notifMenuOpen = false }"
            >
              <span class="inline-flex h-7 w-7 items-center justify-center rounded-full bg-sidebar-bg text-xs font-semibold text-white">
                {{ authStore.userInitials }}
              </span>
              <span class="hidden text-sm font-medium text-slate-800 sm:inline">
                {{ authStore.user?.nome || 'Operador' }}
              </span>
              <UIcon name="i-lucide-chevron-down" class="h-3.5 w-3.5 text-slate-400" />
            </button>

            <div
              v-if="topUserMenuOpen"
              class="absolute right-0 top-[calc(100%+8px)] z-50 w-56 rounded-xl border border-slate-200 bg-white p-1.5 text-slate-900 shadow-xl"
            >
              <div class="border-b border-slate-100 px-2.5 py-2">
                <p class="truncate text-xs font-semibold text-slate-900">
                  {{ authStore.user?.nome || 'Operador' }}
                </p>
                <p class="truncate text-[11px] text-slate-500">
                  {{ authStore.user?.email }}
                </p>
              </div>

              <div class="mt-1 space-y-0.5">
                <NuxtLink
                  to="/conta"
                  class="flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-100"
                  @click="topUserMenuOpen = false"
                >
                  <UIcon name="i-lucide-user-pen" class="h-4 w-4 text-slate-500" />
                  <span>Editar perfil</span>
                </NuxtLink>

                <button
                  type="button"
                  class="flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-sm font-medium text-slate-700 transition-colors hover:bg-slate-100"
                  @click="handleLogout"
                >
                  <UIcon name="i-lucide-log-out" class="h-4 w-4 text-slate-500" />
                  <span>Sair da conta</span>
                </button>

                <div class="my-1 h-px bg-slate-100" />

                <button
                  type="button"
                  class="flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-sm font-medium text-red-600 transition-colors hover:bg-red-50"
                  @click="openDeleteAccountModal"
                >
                  <UIcon name="i-lucide-trash-2" class="h-4 w-4" />
                  <span>Excluir conta</span>
                </button>
              </div>
            </div>
          </div>

          <UButton
            to="/operacoes/agendar"
            color="blue"
            icon="i-lucide-plus"
            size="md"
          >
            <span class="hidden sm:inline">Nova Operação</span>
          </UButton>
        </div>
      </header>

      <main class="flex-1 p-4 sm:p-6">
        <slot />
      </main>
    </div>

    <!-- Modal Excluir Conta com Confirmação de Senha Atual -->
    <UModal v-model="deleteAccountModalOpen">
      <UCard>
        <template #header>
          <div class="flex items-center justify-between gap-2">
            <div class="flex items-center gap-2 text-red-700">
              <UIcon name="i-lucide-alert-octagon" class="h-5 w-5" />
              <h3 class="text-base font-semibold">
                Confirmar exclusão da conta
              </h3>
            </div>
            <UButton
              color="gray"
              variant="ghost"
              icon="i-lucide-x"
              size="sm"
              @click="deleteAccountModalOpen = false"
            />
          </div>
        </template>

        <div class="space-y-4">
          <UAlert
            v-if="deleteAccountError"
            color="red"
            variant="soft"
            icon="i-lucide-alert-circle"
            :description="deleteAccountError"
          />

          <p class="text-sm leading-relaxed text-slate-600">
            Tem certeza de que deseja excluir/desativar a conta de
            <strong class="text-slate-900">{{ authStore.user?.email }}</strong>?
            Seu acesso será encerrado imediatamente. Para confirmar, informe sua senha atual:
          </p>

          <UFormGroup label="Senha atual" required>
            <UInput
              v-model="deleteAccountPassword"
              :type="showDeletePassword ? 'text' : 'password'"
              placeholder="Digite sua senha atual"
              icon="i-lucide-lock"
              autocomplete="current-password"
              :ui="{ icon: { trailing: { pointer: '' } } }"
              @keyup.enter="confirmDeleteAccount"
            >
              <template #trailing>
                <UButton
                  color="gray"
                  variant="link"
                  :icon="showDeletePassword ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                  :padded="false"
                  @click="showDeletePassword = !showDeletePassword"
                />
              </template>
            </UInput>
          </UFormGroup>
        </div>

        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="gray" variant="outline" @click="deleteAccountModalOpen = false">
              Cancelar
            </UButton>
            <UButton
              color="red"
              icon="i-lucide-trash-2"
              :loading="authStore.loading"
              @click="confirmDeleteAccount"
            >
              Confirmar Exclusão
            </UButton>
          </div>
        </template>
      </UCard>
    </UModal>
  </div>
</template>
