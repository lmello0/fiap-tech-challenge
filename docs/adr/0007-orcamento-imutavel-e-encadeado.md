# ADR 0007 — Orçamento imutável e encadeado

**Status:** aceita

## Decisão
- Os itens da OS (serviços e peças, com *snapshot* de nome e preço) podem ser editados apenas em **Recebida** e **Em diagnóstico**.
- Concluir o diagnóstico congela os itens em um `Budget` **imutável**, calculado automaticamente, e envia o e-mail ao cliente.
- Cada orçamento aponta para o anterior (`previous_budget_id`): `v1 (recusado) ← v2 (aprovado)`. A OS continua sendo uma só, com um único histórico de status.
- Recusar o orçamento inicial devolve a OS ao diagnóstico, com os itens restaurados para revisão.
- Um **reparo adicional** é um novo orçamento só com os itens novos. Aprovado ou recusado, a OS volta a **Em execução**; recusar o adicional não cancela o que já foi aprovado.
- O escopo aprovado é a soma dos orçamentos aprovados.
- O atendente pode registrar a decisão em nome do cliente, informando o canal (`PHONE` ou `IN_PERSON`); o autor fica sempre registrado.

## Consequências
Histórico fiel do que o cliente viu e aprovou, mesmo que preços do catálogo mudem depois.
