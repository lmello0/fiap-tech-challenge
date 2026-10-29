# ADR 0005 — Ciclo tolerado entre `workorder` e `inventory`

**Status:** aceita

## Contexto
A saga de estoque exige eventos nos dois sentidos: `workorder` publica `BudgetApproved`, `WorkOrderCompleted` e `WorkOrderCancelled`, que `inventory` consome;
`inventory` publica `PartsReserved` e `PartsShortage`, que `workorder` consome. O Spring Modulith não aceita dependência circular entre módulos.

## Alternativas
1. Módulo neutro `contracts` com todos os eventos — sem ciclo, mas move os eventos para longe do módulo que os publica.
2. Comando por porta síncrona + resposta por evento — a reserva deixaria de ser puramente por eventos.
3. **Tolerar o ciclo** (escolhida).

## Decisão
O `ModularityTest` filtra as violações e aceita **somente** o ciclo cujo conjunto de módulos seja exatamente `{inventory, workorder}`.
Qualquer outro ciclo continua quebrando o build. Outro teste falha quando esse ciclo deixar de existir, para a exceção não ser esquecida.

## Consequências
Os eventos continuam no módulo que os publica. Em microsserviços, o acoplamento já é só por mensagem, então o ciclo deixa de existir de fato.
