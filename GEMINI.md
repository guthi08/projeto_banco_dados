# GEMINI.md — Diretrizes de Engenharia e Assistência Full-Stack para Gemini e Antigravity (v2)

Este documento estabelece o guia unificado de diretrizes de desenvolvimento, arquitetura, convenções e contratos de integração para o **Gemini** e para o ambiente de desenvolvimento **Antigravity** no projeto de **Sistema de Monitoramento e Gestão de Voos com Drones**.

Ele integra integralmente as especificações de **Back-end (Java 21 / Spring Boot / MySQL / Docker)** e **Front-end (Nuxt 3 / Nuxt UI / Tailwind / Pinia / Zod)**.

---

## 🎯 1. Identidade, Idioma e Governança

- **Idioma Principal:** Português do Brasil (documentação, comentários de código, mensagens de commit, mensagens de erro da API e interface do usuário).
- **Convenções de Nomenclatura:**
  - **Back-end:** Pacotes e termos técnicos em inglês (`controller`, `service`, `repository`, `dto`, `entity`, `config`). Entidades de domínio em português correspondentes ao modelo relacional do MySQL Workbench (`EmpresaOperadora`, `Drone`, `Piloto`, `Cliente`, `OperacaoVoo`, `Auditoria`).
  - **Front-end:** Código, composables e schemas em inglês/camelCase (`useApi`, `useDateHelper`, `operacaoVooSchema`). Nomes de páginas e rotas em português no plural (`/empresas`, `/drones`, `/pilotos`, `/operacoes`, `/auditorias`).

---

## 💻 2. Stack Tecnológica Unificada

### 2.1. Back-end (API RESTful)
- **Linguagem & Runtime:** Java 21 LTS
- **Framework:** Spring Boot (Maven)
  - `spring-boot-starter`
  - `spring-boot-starter-web` (Spring MVC)
  - `spring-boot-starter-data-jpa` (Hibernate / JPA)
  - `spring-boot-starter-security` + JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
  - `spring-boot-starter-validation` (Bean Validation)
  - `spring-boot-devtools`
  - `lombok`
  - `spring-boot-starter-test` (JUnit 5, Mockito)
  - `mysql-connector-j` (Driver de conexão com o MySQL Server 8.x)
- **Banco de Dados:** MySQL 8.x (projeto físico gerado no MySQL Workbench)
- **Infraestrutura:** Docker & Docker Compose (`drone-api` e `mysql-db`)

### 2.2. Front-end (Nuxt 3 SPA/SSR)
- **Framework Base:** Nuxt 3 (Vue 3 + Composition API + TypeScript)
- **Design System & Estilização:** Nuxt UI (`@nuxt/ui`) + Tailwind CSS
- **Gerenciamento de Estado:** Pinia (sessão, token JWT, filtros globais)
- **Formulários e Validação:** Zod + `UForm` (Nuxt UI) ou VeeValidate
- **Manipulação de Datas:** Day.js (ou date-fns) para cálculo de prazos ANAC/CMA
- **Utilitários e Reatividade:** VueUse (`@vueuse/core`) para debounce e helpers
- **Visualização de Dados:** Chart.js (`vue-chartjs`)
- **Visualização de Agenda:** V-Calendar ou FullCalendar
- **Comunicação HTTP:** `$fetch` / `useFetch` com interceptor tipado

---

## 🏛️ 3. Arquitetura de Pastas e Componentes

### 3.1. Estrutura do Back-end (`src/main/java/com/drone/api/`)
```
src/main/java/com/drone/api/
├── DroneApiApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── JwtAuthenticationFilter.java
│   └── OpenApiConfig.java
├── controller/
│   ├── AuthController.java
│   ├── EmpresaOperadoraController.java
│   ├── DroneController.java
│   ├── PilotoController.java
│   ├── ClienteController.java
│   ├── OperacaoVooController.java
│   └── AuditoriaController.java
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── OperacaoVooRequest.java
│   │   └── ...
│   └── response/
│       ├── TokenResponse.java
│       ├── OperacaoVooResponse.java
│       └── ...
├── entity/
│   ├── EmpresaOperadora.java
│   ├── Drone.java
│   ├── Piloto.java
│   ├── TelefonePiloto.java
│   ├── Cliente.java
│   ├── TelefoneCliente.java
│   ├── OperacaoVoo.java
│   └── Auditoria.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── RegraDeNegocioException.java
│   └── RecursoNaoEncontradoException.java
├── repository/
│   ├── EmpresaOperadoraRepository.java
│   ├── DroneRepository.java
│   ├── PilotoRepository.java
│   ├── ClienteRepository.java
│   ├── OperacaoVooRepository.java
│   └── AuditoriaRepository.java
└── service/
    ├── AuthService.java
    ├── OperacaoVooService.java
    └── ...
```

### 3.2. Estrutura do Front-end (`frontend/`)
```
frontend/
├── assets/
│   └── css/
│       └── tailwind.css            # Customizações do tema Tailwind e Nuxt UI
├── components/
│   ├── common/                     # Componentes genéricos (StatusBadge, StatCard, ConfirmModal)
│   ├── forms/                      # Formulários desacoplados (VooForm, DroneForm, PilotoForm)
│   ├── tables/                     # Tabelas do Nuxt UI com ordenação e paginação
│   └── charts/                     # Gráficos em Chart.js (StatusChart, OcorrenciasChart)
├── composables/
│   ├── useApi.ts                   # Cliente HTTP tipado com injeção automática de Bearer Token
│   ├── useDateHelper.ts            # Utilitários Day.js (formatação ISO e alertas de validade)
│   └── useDebounceSearch.ts        # Debounce via VueUse para buscas dinâmicas
├── layouts/
│   ├── default.vue                 # Layout principal com Sidebar responsiva e Topbar
│   └── auth.vue                    # Layout para Login e Recuperação de Senha
├── middleware/
│   ├── auth.ts                     # Proteção de rotas privadas (validação de token JWT)
│   └── guest.ts                    # Redireciona usuários logados para o painel principal
├── pages/
│   ├── index.vue                   # Dashboard analítico com métricas e gráficos
│   ├── auth/
│   │   ├── login.vue               # Tela de Login JWT
│   │   ├── register.vue            # Cadastro de nova empresa/operador
│   │   └── forgot-password.vue     # Recuperação de senha
│   ├── empresas/                   # Listagem e edição de operadoras
│   ├── drones/                     # Gestão da frota e monitoramento de cadastro ANAC
│   ├── pilotos/                    # Gestão de pilotos e controle de validade do CMA
│   ├── clientes/                   # Gestão de clientes e contratos de prestação
│   ├── operacoes/
│   │   ├── index.vue               # Painel de operações com visão Tabela / Calendário
│   │   ├── agendar.vue             # Agendamento de missão com validação pré-voo
│   │   └── [id].vue                # Ficha detalhada do voo e lançamento de ocorrências
│   └── auditorias/                 # Histórico e fila de ocorrências técnicas por gravidade
├── stores/
│   ├── auth.ts                     # Pinia: dados do usuário, token JWT e permissões
│   └── filters.ts                  # Pinia: filtros persistentes de período e status
├── types/
│   ├── api.d.ts                    # Interfaces de paginação e erro da API
│   └── models.d.ts                 # Interfaces TypeScript espelhando os DTOs do Spring Boot
└── schemas/
    ├── auth.schema.ts              # Schemas Zod para autenticação
    ├── drone.schema.ts             # Schemas Zod para drones e licença ANAC
    ├── piloto.schema.ts            # Schemas Zod para pilotos e CMA
    └── operacao.schema.ts          # Schemas Zod para agendamento de voos
```

---

## 📦 4. Módulos do Sistema (Visão Full-Stack)

### 4.1. Módulos do Back-End (API Spring Boot)
1. **MOD-BE 1 · Autenticação & Conta (`/api/v1/auth`, `/api/v1/account`):** Cadastro, login JWT, recuperação e desativação lógica.
2. **MOD-BE 2 · Empresas Operadoras (`/api/v1/empresas`):** Gestão cadastral (CNPJ/CPF, razão social, setor, status contratual).
3. **MOD-BE 3 · Drones da Frota (`/api/v1/drones`):** Cadastro de aeronaves, modelo, tipo de uso, peso e validade ANAC.
4. **MOD-BE 4 · Pilotos (`/api/v1/pilotos`):** Cadastro de pilotos, licenças, telefones e validade do CMA.faça
5. **MOD-BE 5 · Clientes (`/api/v1/clientes`):** Gestão de clientes tomadores de serviço e múltiplos telefones.
6. **MOD-BE 6 · Operações de Voo (`/api/v1/operacoes`):** Agendamento e despacho de missões (drone, piloto, cliente, origem/destino, horários e status).
7. **MOD-BE 7 · Auditorias Técnicas (`/api/v1/auditorias`):** Ocorrências, gravidade e conformidade de cada voo.

### 4.2. Módulos do Front-End (Nuxt 3)
1. **MOD-FE 1 · Autenticação & Sessão do Operador (`/auth/...`, `/conta/...`):**
   - **Telas:** Login, Cadastro de Operadora, Recuperação de Senha, Perfil.
   - **Tecnologias:** `UForm` + Zod (`auth.schema.ts`), Pinia (`useAuthStore`), Cookie seguro.
   - **UX:** Validação de força de senha, feedback visual de erros e redirecionamento automático por middleware.
2. **MOD-FE 2 · Painel Geral & Métricas (`/` Dashboard):**
   - **Telas:** Visão geral da operação em tempo real.
   - **Tecnologias:** Nuxt UI (Cards, Badges) + Chart.js (`vue-chartjs`).
   - **UX:** Gráfico doughnut de status de voos (`AGENDADO`, `EM_ANDAMENTO`, etc.), gráfico de barras com voos no mês, contadores de aeronaves disponíveis e incidentes críticos.
3. **MOD-FE 3 · Gestão de Empresas Operadoras (`/empresas`):**
   - **Telas:** Listagem paginada, visualização de contrato, modal de cadastro.
   - **Tecnologias:** `UTable` do Nuxt UI, debounce via VueUse (`useDebounceSearch`), Zod.
   - **UX:** Filtro instantâneo por CNPJ/Razão Social, toggle de status `Ativo`/`Inativo`.
4. **MOD-FE 4 · Frota de Drones & Controle ANAC (`/drones`):**
   - **Telas:** Catálogo da frota, cadastro de aeronave e inspeção.
   - **Tecnologias:** `UTable`, Day.js, Nuxt UI Badges.
   - **UX / Regra Visual:** Cálculo automático de vencimento ANAC com Day.js:
     - 🟢 Válido | 🟡 Vence em < 30 dias | 🔴 Vencido (bloqueia seleção no agendamento).
5. **MOD-FE 5 · Pilotos & Controle de CMA (`/pilotos`):**
   - **Telas:** Gestão de pilotos, licenças e telefones.
   - **Tecnologias:** `UTable`, Day.js, Zod (`piloto.schema.ts`).
   - **UX / Regra Visual:** Sinalização do Certificado Médico Aeronáutico (CMA). Piloto com CMA expirado é desabilitado visualmente para novas escalas.
6. **MOD-FE 6 · Clientes & Contratos (`/clientes`):**
   - **Telas:** Diretório de clientes e histórico de voos por tomador.
   - **Tecnologias:** `UTable`, Nuxt UI Modal, Zod.
   - **UX:** Busca reativa por CPF/RG ou Nome, visualização de contratos vinculados.
7. **MOD-FE 7 · Operações de Voo — Agenda & Despacho (`/operacoes`):**
   - **Telas:** Painel principal (Tabela / Calendário), agendamento e detalhe da missão.
   - **Tecnologias:** V-Calendar ou FullCalendar, `UTable`, `USelectMenu`, Day.js, Zod (`operacao.schema.ts`).
   - **UX / Regra:** Alternância entre listagem e calendário mensal/semanal para detectar choques de horário. Formulário de despacho com validação prévia de vigência de drone e piloto.
8. **MOD-FE 8 · Auditoria Técnica & Ocorrências (`/auditorias`):**
   - **Telas:** Fila de ocorrências e lançamento de auditoria.
   - **Tecnologias:** `UTable`, Chart.js, Badges de severidade.
   - **UX:** Badges padronizados por gravidade (`BAIXO`, `MEDIO`, `ALTO`, `CRITICO`), gráfico de incidentes por categoria.

### 4.3. Matriz de Componentes e Bibliotecas por Módulo Front-end

| Módulo Front-end | Nuxt UI / Tailwind | Zod + UForm | Day.js | Chart.js | V-Calendar | Pinia |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **MOD-FE 1 · Auth** | Formulários e Inputs | Validação de credenciais | — | — | — | Token e Usuário |
| **MOD-FE 2 · Dashboard** | Cards e Layout | — | Filtros de período | Gráficos de voos | — | Período ativo |
| **MOD-FE 3 · Empresas** | Tabelas e Modais | Cadastro de empresa | — | — | — | Operadora ativa |
| **MOD-FE 4 · Drones** | Badges e Filtros | Prefixo ANAC e peso | Checagem ANAC | — | — | — |
| **MOD-FE 5 · Pilotos** | Badges e Listas | CPF e licença | Checagem CMA | — | — | — |
| **MOD-FE 6 · Clientes** | Tabela e Ficha | Cadastro de cliente | — | — | — | — |
| **MOD-FE 7 · Operações** | Tabela e Stepper | Validação do agendamento | Prazos e decolagem | — | Calendário | Filtros de status |
| **MOD-FE 8 · Auditorias**| Badges de gravidade | Registro de ocorrência | Timestamp do evento | Severidade | — | — |

---

## 🔌 5. Contrato de Integração Back-End ↔ Front-End

### 5.1. Autenticação e Interceptor HTTP (`useApi.ts`)
A API Spring Boot opera de forma stateless com Bearer JWT no header `Authorization`. O composable do Nuxt 3 centraliza a injeção do token e o tratamento de sessão:

```typescript
// frontend/composables/useApi.ts
export const useApi = () => {
  const config = useRuntimeConfig();
  const authStore = useAuthStore();

  return $fetch.create({
    baseURL: config.public.apiBaseUrl || 'http://localhost:8080/api/v1',
    onRequest({ options }) {
      if (authStore.token) {
        options.headers = {
          ...options.headers,
          Authorization: `Bearer ${authStore.token}`,
        };
      }
    },
    onResponseError({ response }) {
      if (response.status === 401) {
        authStore.logout();
        navigateTo('/auth/login');
      }
    }
  });
};
```

### 5.2. Paginação Padronizada
Os endpoints de listagem retornam a estrutura do `Page<T>` do Spring Data JPA, consumida diretamente pelo componente `UTable` do Nuxt UI:
```json
{
  "content": [ ... ],
  "totalElements": 48,
  "totalPages": 5,
  "size": 10,
  "number": 0
}
```

### 5.3. Tratamento de Erros e Validação Estruturada
Quando a validação (`@Valid` no back-end ou Zod no front-end) falhar, o formato JSON padronizado é:
```json
{
  "timestamp": "2026-11-21T10:00:00Z",
  "status": 422,
  "error": "Erro de Regra de Negócio",
  "message": "O drone informado possui cadastro na ANAC expirado.",
  "path": "/api/v1/operacoes"
}
```

---

## 🎨 6. Padrões Visuais e Regras de Negócio

### 6.1. Badges de Status de Operação
Os enums retornados pelo Spring Boot são mapeados para componentes visuais padronizados do Nuxt UI:

| Status (`status_voo`) | Variante Nuxt UI | Visual Tailwind |
| :--- | :--- | :--- |
| `AGENDADO` | `color="blue"` | `bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200` |
| `EM_ANDAMENTO` | `color="amber"` | `bg-amber-100 text-amber-800 animate-pulse` |
| `CONCLUIDO` | `color="green"` | `bg-emerald-100 text-emerald-800 dark:bg-emerald-900` |
| `CANCELADO` | `color="red"` | `bg-rose-100 text-rose-800 dark:bg-rose-900` |

### 6.2. Travas de Segurança Aeronáutica (ANAC e CMA)
1. **Validade de Drone:** Proibido agendar ou iniciar voo se a `data_validade_cadastro` do drone for anterior à data do voo. No front-end, o drone aparece desabilitado no formulário com aviso visual via Day.js.
2. **Validade de Piloto:** Proibido escalar piloto com `validade_cma` vencida em relação à data da missão.
3. **Bloqueio de Conflito de Horário:** Drone e piloto não podem ter duas operações com status `AGENDADO` ou `EM_ANDAMENTO` no mesmo intervalo temporal.
4. **Auditorias Imutáveis:** Ocorrências técnicas registradas em `/api/v1/auditorias` exigem vínculo com uma operação existente e nível de severidade (`BAIXO`, `MEDIO`, `ALTO`, `CRITICO`).

---

## ⚡ 7. Comandos Essenciais

### Back-end & Infraestrutura
```bash
# Subir MySQL no Docker
docker compose up -d mysql-db

# Rodar a API Spring Boot em desenvolvimento
./mvnw clean spring-boot:run

# Rodar todos os testes unitários e de integração
./mvnw clean test

# Rodar teste específico
./mvnw test -Dtest=OperacaoVooServiceTest

# Parar os containers Docker
docker compose down
```

### Front-end (Nuxt 3)
```bash
# Instalar dependências
npm install # ou pnpm install / yarn install

# Executar servidor de desenvolvimento
npm run dev

# Fazer build de produção
npm run build

# Executar preview de produção
npm run preview
```

---

## 🚫 8. Escopo Negativo Explícito
- ❌ **Sem Telemetria de Sensores/Bateria em Tempo Real:** Não implementar WebSockets, brokers MQTT ou rotinas de monitoramento de voltagem, amperagem ou saúde das baterias.
- ❌ **Sem Mapas Interativos em Tempo Real com GPS:** Não integrar Mapbox, Leaflet ou Google Maps com tracking contínuo de aeronaves. O sistema trata origem, destino e trajeto de forma declarativa e cadastral.
