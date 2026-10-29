# ADR 0011 — Idempotência dos consumidores

**Status:** aceita

## Contexto
A entrega de eventos é *at-least-once* (e será em RabbitMQ), então todo consumidor precisa tolerar repetição.

## Decisão
- Operações naturalmente idempotentes não usam tabela de controle: atualizar *snapshots* (grava o valor recebido), criar a conta no Keycloak (procura antes pelo `customer_id`) e a reserva de estoque (`budgetId` único).
- Resultados da saga que chegam repetidos, obsoletos ou fora de ordem são **ignorados** pela OS, em vez de falhar.
- O módulo `notification`, cuja ação (enviar e-mail) não é idempotente, usa a tabela `notification.processed_event`: o evento é marcado na mesma transação do envio; se o envio falha, a marca é desfeita e o evento é reprocessado.

## Consequências
Sem e-mail duplicado em reentrega, sem tabela de controle onde ela não é necessária.
