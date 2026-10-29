# Workshop — Sistema Integrado de Atendimento e Execução de Serviços

MVP do back-end de uma oficina mecânica de médio porte (Tech Challenge FIAP, Fase 1). Gerencia clientes, veículos,
catálogo de serviços, estoque de peças e **ordens de serviço (OS)** — da abertura ao orçamento, aprovação pelo cliente,
execução, finalização e entrega — com acompanhamento em tempo real pelo próprio cliente via API.

## Objetivos

- Acabar com a priorização errada de atendimentos, o controle falho de peças e a perda de histórico de clientes e veículos.
- Permitir que o cliente acompanhe o status da OS e **aprove ou recuse orçamentos** (inclusive reparos adicionais) pela API.
- Dar à oficina uma gestão interna eficiente e segura: perfis de acesso, trilha de auditoria e métrica de tempo médio de execução.

## Tecnologias

| Item | Escolha |
|---|---|
| Linguagem / framework | Java 25, Spring Boot 4.1 |
| Arquitetura | Monólito modular, Clean Architecture, orientado a eventos (Spring Modulith) |
| Banco de dados | PostgreSQL 18 — um schema por módulo ([justificativa](docs/justificativa-banco.md)) |
| Migrations | Flyway, uma instância por módulo |
| Autenticação | Keycloak 26 (JWT + refresh token) |
| E-mail | SMTP — Mailpit no ambiente local |
| Documentação da API | OpenAPI / Swagger UI (springdoc) |
| Testes | JUnit 5, AssertJ, Testcontainers, JaCoCo (mínimo de 80% em domínio e aplicação) |
| Container | Dockerfile multi-stage e `compose.yaml` |

## Como executar

Pré-requisito: Docker com Compose.

```bash
cp .env.example .env        # valores de demonstração
docker compose up --build   # sobe postgres, keycloak, mailpit e a aplicação
```

Quando todos os serviços estiverem saudáveis:

| Serviço | URL |
|---|---|
| API / Swagger UI | http://localhost:8080/swagger-ui.html |
| Keycloak (console `admin` / `admin`) | http://localhost:8081 |
| Mailpit (e-mails enviados) | http://localhost:8025 |
| PostgreSQL | `localhost:5432` (banco `workshop`) |

O perfil `demo` vem ativo no compose e carrega dados de exemplo (clientes, veículos, serviços, peças e três OS em estados diferentes).

### Usuários de demonstração

A senha de cada usuário é igual ao seu nome de usuário.

| Usuário | Papel | O que pode fazer |
|---|---|---|
| `admin` | `ADMIN` | Tudo |
| `attendant` | `ATTENDANT` | Cadastros, abrir/entregar/cancelar OS, aprovar orçamento em nome do cliente |
| `mechanic` | `MECHANIC` | Itens, diagnóstico, reparo adicional, finalizar execução |
| `customer` | `CUSTOMER` | Somente a própria área (`/api/v1/me/...`) |

### Obtendo um token

Pelo Swagger UI, use **Authorize** (fluxo *authorization code* com PKCE, cliente `workshop-swagger`). Pela linha de comando:

```bash
TOKEN=$(curl -s -d grant_type=password -d client_id=workshop-cli \
  -d username=attendant -d password=attendant \
  http://localhost:8081/realms/workshop/protocol/openid-connect/token | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/work-orders
```

Para renovar o token, envie o `refresh_token` recebido com `grant_type=refresh_token` ao mesmo endpoint.

## Roteiro de demonstração

1. **Atendente** cadastra um cliente (`POST /api/v1/customers`). A conta de acesso é criada no Keycloak e o e-mail para definir a senha chega no Mailpit.
2. **Atendente** cadastra o veículo (`POST /api/v1/customers/{id}/vehicles`) e abre a OS informando CPF/CNPJ e placa (`POST /api/v1/work-orders`).
3. **Mecânico** inicia o diagnóstico, inclui serviços e peças e conclui o diagnóstico: o **orçamento é gerado automaticamente** e enviado por e-mail.
4. **Cliente** consulta a OS (`GET /api/v1/me/work-orders`) e aprova (`POST /api/v1/me/work-orders/{id}/budgets/{budgetId}/approve`).
   O estoque das peças é reservado e a OS vai para **Em execução** — ou **Aguardando peças**, se faltar estoque.
5. **Mecânico** pode propor um **reparo adicional** (`POST /api/v1/work-orders/{id}/additional-budget`), que o cliente aprova ou recusa.
6. **Mecânico** finaliza (`POST .../complete`, o estoque é baixado) e o **atendente** entrega (`POST .../deliver`).
7. **Gestor** consulta `GET /api/v1/work-orders/metrics` para ver o tempo médio de execução e o tempo médio em cada status.

Dados prontos do perfil `demo`: cliente `customer` (CPF `52998224725`), placas `ABC1D23`, `XYZ9A87` e `QWE4R56`,
OS `OS-DEMO-000001` (Recebida), `OS-DEMO-000002` (Aguardando aprovação) e `OS-DEMO-000003` (Entregue).

## Status da OS

`Recebida → Em diagnóstico → Aguardando aprovação → Em execução → Finalizada → Entregue`, mais **Aguardando peças** e **Cancelada**.
Todas as mudanças são feitas pelas ações do sistema e registradas com o autor no histórico. Detalhes em [docs/arquitetura.md](docs/arquitetura.md).

## Testes e qualidade

```bash
./mvnw verify    # testes unitários e de integração (Testcontainers) + verificação de arquitetura + gate de cobertura
```

- A cobertura mínima de **80%** em domínio e aplicação é exigida pelo build (JaCoCo); o resultado fica em `target/site/jacoco`.
- Os testes de arquitetura (`ModularityTest`, `LayeringTest`) impedem que um módulo acesse o interior de outro e que o domínio dependa de framework.
- Os testes de integração precisam do Docker (Postgres, Keycloak e Mailpit sobem via Testcontainers).

Análise de vulnerabilidades (Dependency-Check e Trivy): [docs/relatorio-vulnerabilidades.md](docs/relatorio-vulnerabilidades.md). Em produção, desligue o Swagger UI com `SWAGGER_UI_ENABLED=false`.

## Estrutura do projeto

```
src/main/java/com/fiap/workshop
├── shared/        value objects (CPF/CNPJ, placa, dinheiro), segurança, tratamento de erros
├── customer/      clientes e veículos
├── catalog/       serviços da oficina
├── inventory/     peças, estoque, movimentações e reservas
├── workorder/     ordens de serviço, orçamentos, histórico e métricas
├── identity/      cria e desativa as contas dos clientes no Keycloak
└── notification/  envia os e-mails a partir dos eventos
```

Cada módulo segue o mesmo formato: `domain` (Java puro), `application` (casos de uso e portas), `adapter` (web, eventos, persistência) e `api` (o que outros módulos podem usar).

## Documentação

- [Arquitetura](docs/arquitetura.md) — módulos, camadas, eventos e saga de estoque
- [Linguagem ubíqua](docs/linguagem-ubiqua.md)
- [Event Storming](docs/event-storming.md)
- [Justificativa do banco de dados](docs/justificativa-banco.md)
- [Decisões de arquitetura (ADRs)](docs/adr)
- [Relatório de vulnerabilidades](docs/relatorio-vulnerabilidades.md)

## Convenções

Código, API, logs e e-mails em inglês; documentação em português.
