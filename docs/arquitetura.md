# Arquitetura

## Visão geral

A aplicação é um **monólito modular** orientado a eventos. Cada módulo é um contexto delimitado (DDD) com seu próprio
schema no PostgreSQL, e a comunicação entre módulos acontece por **eventos**, de modo que cada um possa virar um microsserviço
no futuro — a troca do barramento interno por RabbitMQ não exige mudanças nos módulos ([ADR 0004](adr/0004-eventos-com-outbox-do-modulith.md)).

```mermaid
flowchart LR
    subgraph app[Aplicação Spring Boot]
        customer[customer]
        catalog[catalog]
        inventory[inventory]
        workorder[workorder]
        identity[identity]
        notification[notification]
        shared[shared]
    end
    customer -- eventos --> identity
    customer -- eventos de atualização --> workorder
    catalog -- eventos de atualização --> workorder
    inventory -- eventos de atualização --> workorder
    workorder -- consultas --> customer
    workorder -- consultas --> catalog
    workorder -- consultas --> inventory
    workorder <-- saga de estoque --> inventory
    workorder -- eventos --> notification
    inventory -- eventos --> notification
    identity -- Admin API --> kc[(Keycloak)]
    notification -- SMTP --> mail[(Mailpit)]
    app --- pg[(PostgreSQL<br/>1 schema por módulo)]
```

## Camadas de cada módulo (Clean Architecture)

```mermaid
flowchart TB
    web[adapter.in.web<br/>controllers REST] --> usecases
    events[adapter.in.event<br/>listeners] --> usecases
    subgraph application
        usecases[port.in<br/>casos de uso] --> services[service]
        services --> portout[port.out<br/>portas de saída]
    end
    services --> domain[domain<br/>Java puro]
    persistence[adapter.out.persistence<br/>JPA] -. implementa .-> portout
    lookup[adapter.out.lookup / mail / keycloak] -. implementa .-> portout
```

- **domain**: entidades, value objects e regras de negócio. Sem Spring, JPA ou Lombok (verificado pelo `LayeringTest`).
- **application**: casos de uso (uma operação por classe) e as portas de saída. Controla as transações e publica eventos.
- **adapter**: controllers, listeners de eventos, repositórios JPA (entidades separadas do domínio, com mapeadores), clientes de outros serviços.
- **api**: o único pacote que outros módulos podem importar — consultas síncronas somente de leitura (`CustomerQueries`, `CatalogQueries`, `InventoryQueries`) e os eventos (`api.events`).

As fronteiras são verificadas em todo build pelo Spring Modulith (`ApplicationModules.verify`). A única dependência circular
aceita é a da saga de estoque entre `workorder` e `inventory` ([ADR 0005](adr/0005-ciclo-tolerado-workorder-inventory.md)).

## Banco de dados

- Um schema por módulo: `customer`, `catalog`, `inventory`, `workorder`, `notification` e `events` (registro de publicações do Modulith).
- **Um único usuário de banco** (`workshop_app`) para toda a aplicação.
- Uma instância do Flyway por módulo, com o histórico de migrations dentro do próprio schema (`db/migration/<módulo>`).
- Nenhuma chave estrangeira, *join* ou *view* atravessa schemas: um módulo guarda *snapshots* do que precisa dos outros ([ADR 0009](adr/0009-snapshots-e-consistencia-eventual.md)).
- O perfil `demo` aplica também `db/demo/<módulo>`.

```mermaid
erDiagram
    CUSTOMER ||--o{ VEHICLE : possui
    WORK_ORDER ||--o{ WORK_ORDER_ITEM : "itens do diagnóstico"
    WORK_ORDER ||--o{ BUDGET : orcamentos
    BUDGET ||--o{ BUDGET_LINE : linhas
    BUDGET |o--o| BUDGET : "previous_budget_id"
    WORK_ORDER ||--o{ WORK_ORDER_STATUS_HISTORY : historico
    PART ||--o{ STOCK_MOVEMENT : movimentacoes
    STOCK_RESERVATION ||--o{ STOCK_RESERVATION_LINE : linhas
    PART ||--o{ STOCK_RESERVATION_LINE : reservada
```

As relações entre schemas (por exemplo, `WORK_ORDER` → `CUSTOMER`) existem apenas como *snapshots* de dados, nunca como chave estrangeira.

## Ciclo de vida da OS

```mermaid
stateDiagram-v2
    [*] --> RECEIVED: abrir OS
    RECEIVED --> IN_DIAGNOSIS: iniciar diagnóstico
    IN_DIAGNOSIS --> WAITING_APPROVAL: concluir diagnóstico (gera orçamento)
    WAITING_APPROVAL --> IN_DIAGNOSIS: orçamento inicial recusado
    WAITING_APPROVAL --> IN_EXECUTION: aprovado (sem peças ou peças reservadas)
    WAITING_APPROVAL --> WAITING_PARTS: aprovado, falta estoque
    WAITING_PARTS --> IN_EXECUTION: peças reservadas
    IN_EXECUTION --> WAITING_APPROVAL: reparo adicional proposto
    WAITING_APPROVAL --> IN_EXECUTION: adicional aprovado ou recusado
    IN_EXECUTION --> COMPLETED: finalizar
    COMPLETED --> DELIVERED: entregar
    RECEIVED --> CANCELLED
    IN_DIAGNOSIS --> CANCELLED
    WAITING_APPROVAL --> CANCELLED
    WAITING_PARTS --> CANCELLED
    IN_EXECUTION --> CANCELLED
    DELIVERED --> [*]
    CANCELLED --> [*]
```

A máquina de estados vive no agregado `WorkOrder`; uma transição inválida lança uma exceção de domínio (HTTP 409). Cada transição grava
o autor (usuário, papel ou o próprio sistema) no histórico. Detalhes do orçamento em [ADR 0007](adr/0007-orcamento-imutavel-e-encadeado.md).

## Saga de estoque

```mermaid
sequenceDiagram
    participant C as Cliente
    participant W as workorder
    participant I as inventory
    C->>W: aprova o orçamento
    W-->>I: BudgetApproved (peças)
    alt todas as peças disponíveis
        I->>I: reserva tudo
        I-->>W: PartsReserved
        W->>W: OS vai para IN_EXECUTION
    else falta alguma peça
        I-->>W: PartsShortage
        W->>W: OS vai para WAITING_PARTS
        Note over I: entrada de estoque, ajuste positivo<br/>ou cancelamento de outra OS
        I-->>W: PartsReserved (mais antiga primeiro)
    end
    W-->>I: WorkOrderCompleted
    I->>I: baixa o estoque (movimentação CONSUMPTION)
```

Em caso de cancelamento, `WorkOrderCancelled` libera a reserva e já tenta atender as reservas em espera. Veja o [ADR 0008](adr/0008-estoque-reserva-tudo-ou-nada.md).

## Segurança

- A aplicação é apenas *resource server*: valida o JWT emitido pelo Keycloak. O emissor (`iss`) é validado explicitamente, porque o endereço público do Keycloak difere do interno na rede do Docker.
- Papéis: `ADMIN`, `ATTENDANT`, `MECHANIC` e `CUSTOMER`, aplicados por método (`@PreAuthorize`).
- O cliente também tem login. O claim `customer_id` do token liga a conta ao cadastro, e a posse é sempre verificada pelo token, nunca por um identificador vindo da URL. Uma OS de outro cliente responde 404.
- Erros seguem o formato RFC 9457 (`application/problem+json`).
- CPF/CNPJ (dígitos verificadores) e placa (modelo antigo e Mercosul) são validados no domínio.
