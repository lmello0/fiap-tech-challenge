# ADR 0013 — Webhook de decisão de orçamento assinado com HMAC

**Status:** aceita (Fase 2)

## Contexto
O orçamento pode ser aprovado ou recusado por um sistema externo (gateway de mensagens, aplicativo parceiro) que não tem login no Keycloak.

## Decisão
`POST /api/v1/webhooks/budget-decisions`, sem JWT, autenticado por assinatura:

- `X-Timestamp` (segundos desde 1970) e `X-Signature: sha256=<HMAC-SHA256(segredo, timestamp + "." + corpo)>`;
- o corpo cru é verificado **antes** do parse, com comparação em tempo constante;
- janela de 5 minutos nos dois sentidos, para impedir reenvio de requisições capturadas;
- sem segredo configurado, nenhuma requisição é aceita;
- `eventId` torna a operação idempotente: a repetição responde `DUPLICATE` sem efeito, e um evento novo para um orçamento já decidido responde 409;
- a decisão é gravada com o canal e o ator `EXTERNAL`. A equipe não consegue usar esse canal pelo endpoint de funcionários.

O segredo vem da variável `BUDGET_WEBHOOK_SECRET`; no Kubernetes, de um Secret gerado pelo Terraform.

## Consequências
O parceiro precisa implementar a assinatura (a collection Postman traz um exemplo). Não há proteção por IP; a assinatura e o timestamp são a defesa.
