# ADR 0001 — Monólito modular

**Status:** aceita

## Contexto
O desafio pede um back-end monolítico para o MVP, mas a oficina deve poder crescer e, no futuro, ter partes extraídas como microsserviços.

## Decisão
Um único projeto Maven e uma única aplicação, dividida em módulos de negócio (`customer`, `catalog`, `inventory`, `workorder`, `identity`, `notification`),
cada um um pacote de primeiro nível. O **Spring Modulith** verifica as fronteiras em todo build (`ModularityTest`).

## Consequências
- Operação simples (um processo, um banco) e fronteiras explícitas desde o início.
- Um módulo só enxerga de outro o pacote `api` e os eventos; o restante é interno e o build quebra se for acessado.
- Um Maven multi-módulo daria isolamento físico, mas pesaria demais para um MVP.
