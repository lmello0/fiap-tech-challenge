# ADR 0009 — Snapshots e consistência eventual

**Status:** aceita

## Decisão
Um módulo nunca referencia dados de outro por chave estrangeira. A OS guarda *snapshots* de cliente, veículo, serviço e peça.
Enquanto ainda forem editáveis (OS ativa, itens do rascunho), os *snapshots* são atualizados pelos eventos `CustomerUpdated`, `VehicleUpdated`, `ServiceUpdated` e `PartUpdated`.
Orçamentos emitidos **nunca** são tocados.

Os eventos de atualização carregam nome e preço (e não só `PriceChanged`), porque uma mudança de nome também precisa chegar à OS.

## Consequências
Consistência eventual (milissegundos) entre módulos, aceita pelo negócio. Consultas síncronas de leitura ficam restritas às portas `api` (`CustomerQueries`, `CatalogQueries`, `InventoryQueries`).
