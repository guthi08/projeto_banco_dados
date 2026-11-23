<script setup lang="ts">
const highlights = [
  {
    icon: 'i-lucide-shield-check',
    title: 'Conformidade ANAC & SISANT',
    desc: 'Bloqueio automático de aeronaves com cadastro vencido antes da decolagem.'
  },
  {
    icon: 'i-lucide-user-check',
    title: 'Controle de Validade CMA',
    desc: 'Validação contínua do Certificado Médico Aeronáutico para cada escala de piloto.'
  },
  {
    icon: 'i-lucide-clipboard-check',
    title: 'Auditoria e Despacho de Missões',
    desc: 'Histórico completo de operações origem-destino, ocorrências e gravidade técnica.'
  }
]
</script>

<template>
  <div class="grid min-h-screen lg:grid-cols-12">
    <!-- Painel Institucional Escuro (Carbon Navy-Slate) -->
    <aside class="aero-grid-pattern relative hidden flex-col justify-between border-r border-sidebar-border bg-sidebar-bg p-10 text-sidebar-ink lg:col-span-5 lg:flex xl:col-span-5">
      <div>
        <CommonBrandLogo theme="dark" />

        <div class="mt-12 max-w-md">
          <span class="code-mono inline-flex items-center gap-2 rounded-full border border-sidebar-border bg-sidebar-surface px-3 py-1 text-[11px] font-medium uppercase tracking-wider text-blue-300">
            <span class="h-1.5 w-1.5 rounded-full bg-emerald-400" />
            Sistema Operacional de Frota RPAS
          </span>

          <h1 class="mt-5 text-3xl font-bold leading-tight tracking-tight text-white">
            Monitoramento e gestão segura de voos com drones.
          </h1>

          <p class="mt-3 text-sm leading-relaxed text-slate-300">
            Centralize empresas operadoras, aeronaves cadastradas, pilotos habilitados, agendamento de missões e auditorias técnicas em uma única plataforma.
          </p>
        </div>

        <ul class="mt-10 space-y-4">
          <li
            v-for="item in highlights"
            :key="item.title"
            class="flex items-start gap-3.5 rounded-xl border border-sidebar-border bg-sidebar-surface/80 p-4"
          >
            <span class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-blue-600/20 text-blue-400 ring-1 ring-blue-500/30">
              <UIcon :name="item.icon" class="h-4.5 w-4.5" />
            </span>
            <div>
              <h2 class="text-sm font-semibold text-slate-100">
                {{ item.title }}
              </h2>
              <p class="mt-0.5 text-xs leading-relaxed text-slate-400">
                {{ item.desc }}
              </p>
            </div>
          </li>
        </ul>
      </div>

      <div class="mt-10 flex items-center justify-between border-t border-sidebar-border pt-5 text-xs text-slate-400">
        <span class="code-mono">API REST · JWT STATELESS</span>
        <span class="code-mono text-slate-500">v1.0 · RBAC-E 94</span>
      </div>
    </aside>

    <!-- Área do Formulário (Canvas Claro Carbon) -->
    <main class="flex flex-col justify-between bg-canvas px-5 py-8 sm:px-8 lg:col-span-7 lg:px-12 xl:col-span-7">
      <div class="flex items-center justify-between lg:hidden">
        <CommonBrandLogo theme="light" />
      </div>

      <div class="mx-auto my-auto w-full max-w-md py-6">
        <div class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <slot />
        </div>
      </div>

      <footer class="text-center text-xs text-slate-500">
        DRONE-MONITOR · Sistema de Monitoramento e Gestão de Voos com Drones
      </footer>
    </main>
  </div>
</template>
