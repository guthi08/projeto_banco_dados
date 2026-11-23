# AGENTS.md — Guia de Engenharia e Regras de Negócio

Este arquivo orienta agentes autônomos, assistentes de IA e desenvolvedores que operam neste repositório utilizando o **Antigravity**.

---

## 1. Stack e Ferramentas

- **Linguagem:** Java 21 LTS
- **Framework:** Spring Boot (Spring MVC, Spring Data JPA, Spring Security com JJWT, Bean Validation, Lombok, DevTools)
- **Persistência:** MySQL 8 (`mysql-connector-j` e Hibernate)
- **Modelagem de Dados:** MySQL Workbench (as migrações e entidades devem ser fiéis ao modelo relacional gerado no `backup_mydb.sql`)
- **Ambiente Conteinerizado:** Docker Compose para MySQL e build da API

---

## 2. Fluxo Obrigatório de Código

Ao implementar qualquer funcionalidade no Antigravity, respeite o fluxo estrito:
$$\text{Endpoint REST} \longrightarrow \text{DTO (Record)} \longrightarrow \text{Service} \longrightarrow \text{Repository} \longrightarrow \text{Entity JPA} \longrightarrow \text{MySQL}$$

1. **DTOs Imutáveis:** Utilize Java `record` em `com.drone.api.dto.request` e `com.drone.api.dto.response`. Nunca exponha nem receba Entidades JPA diretamente nos Controllers.
2. **Validação Declarativa:** Anote os records com Bean Validation (`@NotNull`, `@NotBlank`, `@Pattern`, `@FutureOrPresent`).
3. **Services Especializados:** Toda regra de negócio reside exclusivamente nos `@Service`. Controllers são finos e apenas orquestram chamadas e códigos HTTP (`ResponseEntity`).
4. **Tratamento de Exceções:** Quando uma regra de negócio for violada, lance exceções personalizadas de domínio (`RegraDeNegocioException`, `RecursoNaoEncontradoException`). O `GlobalExceptionHandler` (`@RestControllerAdvice`) cuidará da resposta HTTP padronizada (400, 404, 409 ou 422).

---

## 3. Regras de Negócio do Domínio Aeronáutico

### 3.1. Empresa Operadora (`Empresa_Ope`)
- Possui CPF ou CNPJ único, Razão Social ou Nome do Responsável, e-mail, telefone(s) e setor de atuação.
- Status do contrato aceita: `Ativo` ou `Inativo`/`Suspenso`.
- Drones e Pilotos são subordinados a uma Empresa Operadora.

### 3.2. Drones (`drone`)
- Identificador no padrão de registro aeronáutico brasileiro (prefixos PR, PP, PS).
- Atributos: fabricante, modelo, número de série, peso máximo de decolagem (kg), tipo de uso (`basico` ou `avancado`) e data de validade do cadastro.
- **Trava de Segurança (ANAC):** O sistema proíbe o agendamento ou início de qualquer operação caso a data do voo seja posterior à `data_validade_cadastro` do drone.

### 3.3. Pilotos (`piloto`)
- Identificador fiscal: CPF (único), nome, número do registro/licença de voo, validade do Certificado Médico Aeronáutico (`validade_cma`), e-mail e múltiplos telefones.
- **Trava de Segurança (CMA):** O sistema proíbe terminantemente a escala de qualquer piloto cujo CMA esteja vencido na data planejada ou real da missão.

### 3.4. Clientes e Relações Comerciais (`clientes`)
- Cadastro com CPF ou RG, nome e telefones.
- O relacionamento entre empresa e cliente registra o `status_contrato` (`Ativo`/`Inativo`) e a data de formalização.

### 3.5. Operações de Voo (`Operacao_voo`)
- Cada operação exige estritamente: **1 drone**, **1 piloto responsável** e **1 cliente solicitante**, todos vinculados à respectiva empresa operadora.
- O trajeto exige a identificação formal de dois locais de operação distintos: **origem** e **destino**, além da descrição da área sobrevoada.
- Status da operação: `AGENDADO`, `EM_ANDAMENTO`, `CONCLUIDO`, `CANCELADO`.
- Armazena datas e horas planejadas e reais de decolagem e pouso.
- **Validações Pré-Voo:**
  - Drone com cadastro na ANAC válido na data da missão.
  - Piloto com CMA válido na data da missão.
  - Bloqueio de sobreposição de horários para a mesma aeronave ou mesmo piloto em operações com status `AGENDADO` ou `EM_ANDAMENTO`.

### 3.6. Auditoria de Missões (`Auditoria`)
- Eventos e ocorrências registrados durante ou após uma operação de voo (ex: desvio de rota, alerta meteorológico, incidente operacional).
- Atributos: código, data/hora do evento, categoria, descrição detalhada e nível de gravidade (`BAIXO`, `MEDIO`, `ALTO`, `CRITICO`).
- Cada auditoria pertence exclusivamente a um único voo.

### 3.7. Fora do Escopo Técnico
- ❌ Não implementar serviços de telemetria IoT em tempo real (saúde da bateria, voltagem, consumo de motores).
- ❌ Não implementar renderização de mapa com GPS em tempo real (os locais de voo são tratados como registros descritivos/pontos operacionais).

---

## 4. Otimização e Índices no MySQL

As consultas nos Repositories devem tirar proveito dos índices criados no projeto físico do MySQL Workbench:
- `idx_modelo_drone` na tabela `drone`
- `idx_nome_cliente` na tabela `clientes`
- `idx_status_voo` na tabela `Operacao_voo`
- `idx_nome_piloto` na tabela `piloto`

---

## 5. Diretrizes para o Antigravity

- Use Java moderno: Records para DTOs, Pattern Matching e Switch Expressions.
- Evite anotações `@Data` do Lombok em Entidades JPA para prevenir loops infinitos em `hashCode`/`equals` com relacionamentos bidirecionais (use `@Getter`, `@Setter` e defina `equals`/`hashCode` baseados no identificador).
- Mantenha anotações explícitas de `@Transactional(readOnly = true)` em métodos de busca e `@Transactional` em alterações de dados.
