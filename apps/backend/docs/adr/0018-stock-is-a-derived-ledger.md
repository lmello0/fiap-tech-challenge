# Stock is a derived ledger, not a mutable column

`inventory.parts` used to carry `quantity_on_hand`, `quantity_reserved`, and `average_cost` as
mutable columns, updated in place by `Part.receive`/`consumeReservation`/`applyAdjustment`, with
`stock_movements` written alongside them as a side log. That meant two places could disagree about
what's on the shelf, and the movement log's completeness was never actually load-bearing — nothing
would break if a write to it were skipped, since the column was the real answer.

We removed the columns. On-hand for a part is now `SUM(stock_movements.quantity)`; nothing else
stores it. `StockLedger` (`inventory.services`) is the only component that writes a movement or reads
a part's derived on-hand/reserved/available, and every reserve/consume/receive/adjust path goes
through it. `average_cost` is gone the same way, replaced by a moving average over `PURCHASE`
movements computed on read (see the `inventory.part_stock` view and ADR 0020's windowed-cost
discussion) — cost is a reporting number nothing blocks on, so recomputing it per read costs nothing
that matters.

The alternative — keeping a materialized balance column as a cache of the ledger, updated
transactionally alongside it — was rejected for this codebase's scale: it reintroduces exactly the
"two sources that can disagree" problem the ledger exists to remove, in exchange for saving an
aggregate query per read. At a repair shop's part volumes, that query is not the bottleneck.

Removing the balance column also removes the row that used to double as a lock. `parts` is now a pure
catalog row with no quantity of its own, but `PartRepository.findByIdForUpdate`'s `SELECT ... FOR
UPDATE` on it is still taken before every reserve/consume/receive/adjust — the row is kept purely as a
per-part mutex token, so two concurrent claims on the last unit of a part still serialize instead of
both reading the same stale `SUM` and both succeeding.

`part_reservations` is deliberately **not** part of this ledger. A reservation has its own lifecycle,
an owning work order, and a shortfall — that's an entity with state, not an event line — and folding
it into `stock_movements` would make `SUM(quantity)` stop meaning "what is physically on the shelf,"
which is the one property the ledger exists to guarantee. `reserved` is a separate `SUM` over `HELD`
reservations, and `available` is `on_hand - reserved`.
