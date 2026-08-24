# Shortfalls heal FIFO on receipt, uniformly across draft and approved work orders

A reservation's shortfall (`quantityRequested - quantityReserved`) used to just sit there until
someone looked at it — `startService` would throw `InsufficientStockException` and that was the
extent of the system's opinion on the matter. There was no way to ask "is this work order still
blocked?" without trying to start it and catching the failure, and receiving the part that was short
did nothing to the reservation that was waiting on it.

`StockLedger.healShortfalls`, called after every stock increase (a `PURCHASE` receipt or a positive
`ADJUSTMENT`), now tops up open shortfalls for that part FIFO by `reservedAt` — whichever reservation
has been waiting longest gets satisfied first, up to however much just arrived. This is what makes
the readiness question answerable directly: `PartReservationService.getBlockingShortfalls` reads the
current state of `part_reservations` rather than replaying history, because receiving stock keeps that
state current instead of leaving it to go stale until someone re-quotes.

**FIFO by wait time, not by work order priority.** Three work orders short on the same part, a
receipt that only covers two: the two that quoted first get satisfied, not the two an attendant might
consider more urgent, and not necessarily the ones closest to being approved. We considered
prioritizing by budget/work-order state — an approved order should probably out-rank a draft nobody
has sent yet — and rejected it on purpose: giving `StockLedger` a rule like "approved beats draft"
means inventory has to know what a budget's status is, which is exactly the dependency ADR 0006's
one-way boundary exists to prevent. The accepted cost is real: a stale draft that happens to have
reserved first can absorb stock ahead of a customer who already said yes. The mitigation already
exists and needed no new code — `ExpireStaleReservations` ages out a reservation nobody ever acted on,
so a forgotten draft eventually stops competing for stock at all. If this cost turns out to matter in
practice, the fix is `workorder` passing a priority hint into `reserveForWorkOrder`, not inventory
reaching across the boundary to read budget state itself.

**Publishes on every partial heal, not only full resolution.** `WorkOrderPartsReplenishedEvent`
carries `remainingShortfallCount` specifically so a listener can tell a work order that's still
short from one that just became startable — firing only on full resolution would mean the event's own
payload (which parts got healed, how many are still short) is only ever `0` and useless. Nothing
consumes this event yet; it exists now so a future listener (e.g. notifying a customer their car can
proceed) doesn't require changing `StockLedger` to add one.
