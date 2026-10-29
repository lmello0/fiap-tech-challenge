# Justificativa do banco de dados — PostgreSQL

## Escolha
**PostgreSQL 18**, em um único servidor com um schema por módulo.

## Por que um banco relacional
O domínio da oficina é fortemente relacional e transacional: ordem de serviço → orçamentos → linhas, histórico de status, saldo de estoque com movimentações e reservas.
Essas regras exigem **transações ACID**, restrições de integridade e consultas de agregação (as métricas de tempo médio usam funções de janela sobre o histórico de status).
Um banco de documentos ou chave-valor perderia as garantias de que o saldo de estoque nunca fica negativo e de que um veículo tem uma única OS ativa.

## Por que PostgreSQL
- **Integridade expressa no banco:** `CHECK` (saldo e reservas não negativos, `reservado <= em estoque`), índices únicos, e **índice único parcial** (`uk_work_order_active_vehicle`: um veículo só pode ter uma OS fora de Entregue/Cancelada).
- **Concorrência:** MVCC e `SELECT ... FOR UPDATE` sustentam os locks pessimistas da saga de estoque, convivendo com o lock otimista das requisições de usuário.
- **Schemas:** isolam cada módulo no mesmo servidor, com migrations e histórico independentes. Cada schema pode ser extraído para outro servidor sem mudar as tabelas.
- **Funções de janela (`lead`)** calculam o tempo em cada status direto do histórico.
- **UUID e `timestamptz`** nativos, adequados a UUIDv7 gerado na aplicação.
- Código aberto, maduro, com imagem oficial e suporte nativo do Spring (Hibernate, Flyway, Testcontainers).

## Como é usado
- Um schema por módulo: `customer`, `catalog`, `inventory`, `workorder`, `notification` e `events`.
- Um único usuário de banco para a aplicação; um banco e um usuário separados para o Keycloak, no mesmo servidor.
- Evolução do esquema por Flyway, uma instância por módulo ([ADR 0003](adr/0003-schema-por-modulo-e-flyway.md)).
- Sem chave estrangeira entre schemas, o que preserva a possibilidade de dividir em microsserviços.
