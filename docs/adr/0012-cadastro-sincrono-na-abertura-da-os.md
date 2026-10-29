# ADR 0012 — Cadastro síncrono de cliente e veículo na abertura da OS

**Status:** aceita (Fase 2)

## Contexto
A API de abertura da OS deve receber os dados do cliente e do veículo, e não exigir cadastros prévios. Até a Fase 1, a regra era "mudança de estado em outro módulo só por evento".

## Decisão
O módulo `customer` expõe a porta síncrona `customer.api.CustomerRegistration` (`ensureCustomer` e `ensureVehicle`):

- cliente existente (pelo documento) ou veículo existente (pela placa) é **reaproveitado sem sobrescrever** os dados;
- se não existir, é cadastrado pelos mesmos casos de uso de antes, então `CustomerRegistered` continua criando a conta no Keycloak;
- cliente ou veículo desativado, veículo de outro cliente, documento ou placa inválidos são recusados (422);
- tudo acontece na transação da abertura: se a abertura falha (por exemplo, item inexistente), cliente e veículo recém-criados também não são gravados.

## Consequências
É a única exceção à regra "estado só por evento": a OS precisa do identificador do cliente na mesma operação. A porta é pequena, em `api`, e pode virar uma chamada HTTP na extração para microsserviços. Os dados do cliente passam a ser enviados em toda abertura, mas só valem para o primeiro cadastro.
