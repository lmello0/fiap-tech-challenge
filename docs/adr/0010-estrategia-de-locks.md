# ADR 0010 — Estratégia de locks

**Status:** aceita

## Contexto
Um teste revelou uma corrida: os listeners que atualizam *snapshots* (cliente, veículo, serviço) rodam em paralelo e alteram a mesma OS.
Com lock otimista, um deles perdia a disputa e o evento ficava pendente para sempre.

## Decisão
- **Requisições de usuário:** lock otimista (`@Version`); o conflito vira HTTP 409 e o cliente repete.
- **Caminhos disparados por eventos** (atualização de *snapshots*, resultado da saga, reservas de estoque): lock pessimista na linha, sempre em ordem de id para evitar *deadlock*. Eles esperam em vez de falhar.

## Consequências
Nenhuma atualização perdida e nenhum evento preso por concorrência. Um teste dispara 12 entradas de estoque simultâneas e confere que o saldo final bate com o número de sucessos.
