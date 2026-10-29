# ADR 0008 — Estoque: movimentações, reserva tudo-ou-nada e fila FIFO

**Status:** aceita

## Decisão
- O saldo físico só muda por **movimentações** auditáveis (`ENTRY`, `ADJUSTMENT`, `CONSUMPTION`), com saldo posterior e autor. Nunca fica negativo.
- **Disponível = em estoque − reservado.** Ajustes manuais não consomem estoque reservado, e o alerta de estoque baixo (`LowStockDetected`) usa o disponível.
- Aprovar um orçamento com peças cria uma reserva **tudo-ou-nada**: se qualquer peça faltar, nada é reservado e a OS aguarda.
- Entrada de estoque, ajuste positivo ou cancelamento de outra OS reavaliam as reservas em espera, **da mais antiga para a mais nova**.
  Uma reserva menor e mais nova não fica presa atrás de uma antiga que ainda não cabe.
- Finalizar a OS baixa o estoque reservado; cancelar libera a reserva.
- O `budgetId` é único na tabela de reservas, o que torna o pedido idempotente.

## Consequências
Não há "estoque fantasma" nem peça prometida a duas OS. Em troca, uma OS grande pode esperar mais do que uma pequena.
