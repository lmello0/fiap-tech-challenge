# ADR 0002 — Clean Architecture dentro de cada módulo

**Status:** aceita

## Decisão
Cada módulo tem `domain` (Java puro), `application` (casos de uso e portas) e `adapter` (web, eventos, persistência, integrações).
As dependências apontam para dentro: adaptadores dependem da aplicação, e a aplicação, do domínio.

- O domínio não usa Spring, JPA, Jackson nem Lombok; entidades JPA são classes separadas, com mapeadores explícitos.
- Casos de uso são uma operação por classe, com uma interface de entrada (`port.in`).
- O `LayeringTest` (ArchUnit) falha o build se o domínio importar framework ou se a aplicação importar um adaptador.

## Consequências
Mais classes e código de mapeamento, em troca de regras de negócio testáveis sem framework (cobertura de domínio e aplicação acima de 99%) e troca fácil de infraestrutura.
