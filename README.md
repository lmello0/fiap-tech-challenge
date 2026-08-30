# Auto Repair Shop 
## Tech Challenge - Fase 1 | Pos-Tech FIAP | Software Architecture

---

Event Storming: [Miro](https://miro.com/app/board/uXjVH9wTsBg=/?share_link_id=625997029412)

## Visão geral

Plataforma de gestão de uma oficina mecânica: clientes e visitantes agendam um drop-off, a equipe
faz o diagnóstico e monta um Orçamento (Budget), o cliente aprova ou recusa, a equipe executa o
serviço reservando peças do estoque, e o cliente retira o veículo. Agendamento (calendário
operacional, drop-off/pickup) e estoque (peças, serviços, compras) sustentam esse ciclo de ponta a
ponta, com um histórico de eventos imutável por agregado.

Modelo de domínio completo em [CONTEXT-MAP.md](CONTEXT-MAP.md) e visão de produto em
[PRODUCT.md](PRODUCT.md).

## Layout

Monorepo.

- `apps/backend/` — API em Spring Boot. Veja `apps/backend/CONTEXT.md` (domínio) e
  `apps/backend/HELP.md` (básico de Maven/Spring Boot).
- `apps/frontend/` — console Angular usado pela equipe da oficina. Veja `apps/frontend/README.md`.
- `apps/db/` — script de inicialização do banco (`init.sql`).
- `backend-openapi.yaml` — contrato da API.

### Módulos do backend (`apps/backend/src/main/java/.../techchallenge/`)

| Módulo | Responsabilidade |
|---|---|
| `auth` | Autenticação e autorização |
| `user` | Usuários e seus papéis (Cliente, Atendente, Mecânico, Gerente, Estoquista) |
| `vehicle` | Veículos dos clientes |
| `workorder` | Ordens de serviço e orçamentos (diagnóstico → orçamento → execução) |
| `inventory` | Peças, serviços, compras e controle de estoque |
| `scheduling` | Agendamentos, calendário operacional e fechamentos |
| `history` | Linha do tempo de eventos por agregado |
| `email` | Envio de notificações (orçamento, convites, recuperação de senha) |
| `shared` | Infraestrutura e utilitários comuns |

### Módulos do frontend (`apps/frontend/src/app/`)

| Módulo | Responsabilidade |
|---|---|
| `core/api` | Cliente da API do backend |
| `core/auth` | Sessão e autenticação no console |
| `core/data` | Acesso a dados e store (`ShopStore`) |
| `core/domain` | Modelos de domínio no frontend |
| `features/work-orders` | Ordens de serviço e orçamentos |
| `features/schedule` | Agendamentos e calendário |
| `features/inventory` | Peças, serviços e estoque |
| `features/records` | Cadastro de clientes, veículos e equipe |
| `features/sign-in` | Autenticação |
| `shared/ui` | Componentes de UI reutilizáveis |

## Como rodar pela primeira vez

Pré-requisitos: Docker e Docker Compose (Node/npm apenas se quiser rodar o frontend fora de
container, via `npm start`).

1. Configure as variáveis de ambiente do backend a partir do exemplo:
   `apps/backend/.env.example` → `apps/backend/.env`.
2. Suba a aplicação completa (Postgres, Mailpit, backend e frontend em containers):

   ```sh
   docker compose --profile app up
   ```

   - Backend: http://localhost:8080
   - Frontend: http://localhost:4200
   - Mailpit (e-mails de dev): http://localhost:8025

   Alternativa para desenvolvimento local do backend/frontend fora de container (só sobe
   Postgres + Mailpit): `docker compose up`.

3. (Opcional) Pipeline de qualidade com SonarQube:

   ```sh
   docker compose --profile sonar up --abort-on-container-exit sonar-analysis
   ```

   Resultados em http://localhost:9000.

## Relatório de vulnerabilidade

Para gerar o relatório de vulnerabilidade (`dependency-check` plugin no `pom.xml`) é necessário gerar um API token no [Site oficial do NIST](https://nvd.nist.gov/developers/request-an-api-key),
após isso, execute o comando:

```sh
./mvnw verify -Dnvd.api.key="<API-KEY>" -DskipTests=true
```

O relatório será um `.html` gerado na pasta raíz em `target/`.

Escolhe deliberadamente deixar esse plugin desabilitado para não impedir a compilação da aplicação

## Links úteis

- Event Storming: [Miro](https://miro.com/app/board/uXjVH6Q299o=/?share_link_id=234351769480)
- [CONTEXT-MAP.md](CONTEXT-MAP.md) — mapa de contextos do domínio
- [PRODUCT.md](PRODUCT.md) — visão de produto
- [backend-openapi.yaml](backend-openapi.yaml) — contrato da API
