# ADR 0004 — Eventos com o registro de publicações do Spring Modulith

**Status:** aceita

## Decisão
Módulos se comunicam por eventos de integração (`<módulo>.api.events`). As publicações são persistidas na mesma transação (padrão *outbox*, schema `events`)
e entregues por `@ApplicationModuleListener` (assíncrono, após o *commit*). Publicações incompletas são reenviadas na reinicialização.

Os eventos são registros imutáveis, com `eventId` (UUIDv7), `occurredAt` e apenas tipos primitivos, identificadores e *snapshots*, e já levam `@Externalized("workshop.<módulo>::<evento>")`.

## Consequências
- Entrega *at-least-once*: os consumidores precisam tolerar repetição ([ADR 0011](0011-idempotencia-dos-consumidores.md)).
- Para extrair um módulo para RabbitMQ basta adicionar o módulo de externalização do Modulith; os módulos não mudam.
- Evolução dos eventos só aditiva: campos novos são opcionais e nada é removido.
