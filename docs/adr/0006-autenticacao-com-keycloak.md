# ADR 0006 — Autenticação com Keycloak

**Status:** aceita

## Decisão
O Keycloak emite os JWTs (access e refresh token); a aplicação é só *resource server*.

- Papéis do realm: `ADMIN`, `ATTENDANT`, `MECHANIC`, `CUSTOMER`.
- **O cliente também tem login.** A conta é criada apenas pelo atendente: o evento `CustomerRegistered` faz o módulo `identity` criar o usuário via Admin API
  (e-mail para definir a senha) e o claim `customer_id` liga o token ao cadastro. Desativar o cliente desabilita a conta.
- A posse é verificada pelo `customer_id` do token, nunca por um id da URL; a OS de outro cliente responde 404.
- Funcionários são gerenciados no console do Keycloak, sem endpoints próprios.
- O realm é importado de `infra/keycloak/realm-workshop.json`. O `iss` público (`localhost:8081`) difere do endereço interno, então o emissor é validado explicitamente e as chaves são buscadas pelo endereço interno.

## Consequências
Sem código de senha, sessão ou refresh na aplicação. O fluxo cliente → Keycloak é independente do restante, e falhas do Keycloak não derrubam o cadastro, pois o evento é reprocessado.
