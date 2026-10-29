# ADR 0003 — Um schema por módulo, um usuário de banco, Flyway por módulo

**Status:** aceita

## Decisão
- Cada módulo é dono de um schema do PostgreSQL. A aplicação usa **um único usuário** de banco (`workshop_app`).
- Uma instância do Flyway por módulo, com `schemas=<módulo>`, migrations em `db/migration/<módulo>` e `flyway_schema_history` dentro do próprio schema.
- Nenhuma chave estrangeira, *join* ou *view* entre schemas.
- O Hibernate roda com `ddl-auto=validate`, depois de todas as migrations.

## Consequências
Cada schema é autocontido e pode sair junto com o seu serviço. Integridade entre módulos é responsabilidade dos eventos e dos *snapshots* ([ADR 0009](0009-snapshots-e-consistencia-eventual.md)), não do banco.
