# DRONE-MONITOR — Sistema de Monitoramento e Gestão de Voos com Drones

Plataforma Full-Stack desenvolvida em **Java 21 + Spring Boot 3** (Back-End RESTful), **Nuxt 3 + Vue 3 + Nuxt UI + Tailwind CSS** (Front-End), **MySQL 8** (persistência relacional) e **Docker / Docker Compose**, projetada para planejar, controlar, validar e auditar operações comerciais de voo com drones (RPAS) em conformidade com as normas da ANAC.



---

## Dados de Acesso para Avaliação

Ao iniciar o sistema contra um banco vazio (`drone_monitor`), o Spring Boot cria automaticamente todas as tabelas, índices, chaves estrangeiras e carrega os dados iniciais (3 empresas operadoras, drones válidos/vencidos, pilotos com CMA válido/vencido, clientes, operações de voo e auditorias técnicas):

| Perfil | Como acessar | E-mail | Senha | Permissões |
| :--- | :--- | :--- | :--- | :--- |
| **Administrador (`ADMIN`)** | Na tela de login, clique em **"Acesso Administrador"** | `operacoes@aerologbrasil.com.br` | `Aero@2026` | Visão global de todas as empresas, drones, pilotos, clientes, operações e auditorias. |
| **Empresa Operadora (`OPERADOR`)** | Clique em **"Cadastrar operadora"** (`/auth/register`) e depois faça login comum | _(e-mail cadastrado)_ | _(senha criada)_ | Isolamento multi-tenant: acessa e gerencia apenas os recursos vinculados à própria empresa. |

---

## Como Baixar e Instalar o Projeto

### 1. Obter o código-fonte
Você pode baixar o projeto de duas formas:

- **Via Git (recomendado):**
  ```bash
  git clone https://github.com/SEU_USUARIO/drone-monitor.git
  cd drone-monitor
  ```
- **Via ZIP (GitHub ou Google Drive):**
  1. Baixe o arquivo compactado do projeto e extraia em uma pasta de sua preferência.
  2. Abra um terminal dentro da pasta raiz extraída (onde está localizado o arquivo `docker-compose.yml`).

### 2. Instalar os pré-requisitos
Escolha o modo de execução desejado abaixo:
- **Para executar via Docker Compose (mais simples):** instale apenas o [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows/macOS) ou o **Docker Engine + Docker Compose Plugin** (Linux). Não é necessário instalar Java, Maven, Node.js ou MySQL na máquina.
- **Para executar localmente (sem Docker):** instale **Java 21 LTS (JDK)**, **Apache Maven 3.8+**, **Node.js 20+ (com npm)** e **MySQL Server 8.0+**.

---

## Como Executar o Projeto

### Opção 1 — Docker Compose (Recomendado — sobe MySQL + API + Front-End juntos)

1. *(Opcional)* Se você já possui um MySQL rodando localmente na porta `3306`, copie `.env.example` para `.env` e altere `MYSQL_PORT=3307`:
   ```bash
   cp .env.example .env
   ```
2. Na pasta raiz do projeto, construa as imagens e suba todos os containers (na primeira execução o Docker baixa automaticamente as dependências do Back-End e Front-End):
   ```bash
   docker compose up -d --build
   ```
3. Acompanhe a inicialização até a API ficar pronta:
   ```bash
   docker compose logs -f drone-api
   ```
4. Acesse no navegador:
   - **Aplicação Web (Front-End):** `http://localhost:3000`
   

Para parar os containers (mantendo os dados no volume):
```bash
docker compose down
```
Para parar e zerar completamente o banco de dados:
```bash
docker compose down -v
```

---

### Opção 2 — Execução Local (sem Docker)

1. **Crie o banco de dados vazio no MySQL 8:**
   ```sql
   CREATE DATABASE IF NOT EXISTS drone_monitor
     CHARACTER SET utf8mb4
     COLLATE utf8mb4_unicode_ci;

   CREATE USER IF NOT EXISTS 'drone_user'@'localhost' IDENTIFIED BY 'SUA_SENHA';
   GRANT ALL PRIVILEGES ON drone_monitor.* TO 'drone_user'@'localhost';
   FLUSH PRIVILEGES;
   ```
   *(Não é necessário criar tabelas manualmente — o Hibernate cria automaticamente todas as tabelas e o `AuthDataInitializer` popula os dados iniciais na primeira execução.)*

2. **Instale as dependências e inicie o Back-End (Terminal 1 — porta `8080`):**
   ```bash
   cd back-end
   mvn dependency:go-offline
   SPRING_DATASOURCE_USERNAME="drone_user" SPRING_DATASOURCE_PASSWORD="SUA_SENHA" mvn spring-boot:run
   ```

3. **Instale as dependências e inicie o Front-End (Terminal 2 — porta `3000`):**
   ```bash
   cd front-end
   npm install
   npm run dev
   ```

4. Acesse **`http://localhost:3000`** no navegador.

---

## Stack usada 

### Back-End (`back-end/`)
- **Linguagem & Framework:** Java 21 LTS · Spring Boot 3.4.3 (Spring Web, Spring Data JPA, Spring Security, Bean Validation)
- **Autenticação & Segurança:** JWT Stateless (`JJWT 0.12.6`) + BCrypt + isolamento multi-tenant (`ADMIN` × `OPERADOR`)
- **Persistência:** MySQL 8 (`mysql-connector-j`) + Hibernate ORM
- **Testes:** JUnit 5, Mockito, MockMvc e H2 (modo MySQL)

### Front-End (`front-end/`)
- **Framework:** Nuxt 3 (Vue 3 Composition API + TypeScript)
- **Interface:** Nuxt UI (`@nuxt/ui`) + Tailwind CSS
- **Estado & Validação:** Pinia + Zod + `@vueuse/core`
- **Visualização & Datas:** Chart.js (`vue-chartjs`) + Day.js

---

## Arquitetura e Fluxo de Código

```text
Front-End (Nuxt 3) ──► Security Filter (JWT) ──► Controller ──► DTO (Record) ──► Service (Regras) ──► Repository ──► Entity JPA ──► MySQL 8
```

### Módulos Implementados
1. **Autenticação & Conta (`/api/v1/auth`, `/api/v1/account`):** Cadastro de operadoras (`OPERADOR`), login comum e Acesso Administrador (`ADMIN`), recuperação de senha (token UUID com validade de 30 min registrado no log do servidor em ambiente local), edição de perfil e exclusão/desativação lógica mediante confirmação obrigatória da senha atual.
2. **Empresas Operadoras (`/api/v1/empresas`):** Listagem paginada, filtros por documento/razão social/setor/status, cadastro, edição e alteração de status contratual (`Ativo`, `Inativo`, `Suspenso`) — gestão global exclusiva para `ADMIN`.
3. **Drones da Frota (`/api/v1/drones`):** CRUD completo com validação de prefixo aeronáutico brasileiro (`PR`, `PP`, `PS`), índice `idx_modelo_drone`, controle de peso máximo de decolagem e semáforo de validade ANAC.
4. **Pilotos (`/api/v1/pilotos`):** CRUD completo com CPF/RG/licença únicos, múltiplos telefones (`telefone_piloto`), índice `idx_nome_piloto` e controle de validade do CMA (`validade_CMA`).
5. **Clientes (`/api/v1/clientes`):** CRUD completo com CPF/CNPJ e RG únicos, múltiplos telefones (`telefone_cliente`), índice `idx_nome_cliente` e controle de status contratual (`Ativo`/`Inativo`).
6. **Operações de Voo (`/api/v1/operacoes`):** Agendamento, visão Tabela e Calendário, e ciclo de vida (`AGENDADO` → `EM_ANDAMENTO` → `CONCLUIDO` / `CANCELADO`) com índice `idx_status_voo` e validações pré-voo obrigatórias:
   - **Trava ANAC:** bloqueia agendamento ou início de voo com drone cujo cadastro esteja expirado (`422`).
   - **Trava CMA:** bloqueia escala de piloto com Certificado Médico Aeronáutico vencido (`422`).
   - **Bloqueio de Conflito de Horário:** impede sobreposição temporal para o mesmo drone ou mesmo piloto em operações ativas (`409`).
   - **Consistência Operacional:** origem e destino distintos, contrato do cliente ativo e vínculo à mesma operadora.
7. **Auditorias Técnicas (`/api/v1/auditorias`):** Registro imutável de ocorrências vinculadas a uma operação existente, classificação por gravidade (`BAIXO`, `MEDIO`, `ALTO`, `CRITICO`), gráficos por categoria e rastreabilidade por empresa para o `ADMIN`.

> **Escopo Negativo:** Conforme a especificação do projeto, não há telemetria IoT de sensores/bateria em tempo real nem rastreamento contínuo em mapa GPS (origem, destino e área são registrados de forma declarativa).

---

## Testes Automatizados

### Back-End (31 testes — JUnit 5, Mockito e MockMvc)
```bash
mvn -f back-end/pom.xml test
```
Cobre autenticação JWT, confirmação de senha na conta, isolamento multi-tenant (`OPERADOR` × `ADMIN`), CRUDs de todos os módulos e todas as travas aeronáuticas (`OperacaoVooServiceTest`).

### Front-End (TypeScript e Build de Produção)
```bash
cd front-end
npm run typecheck
npm run build
```
