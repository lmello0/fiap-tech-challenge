# Reserving is best-effort, consuming is strict

A quote that needs four brake pads when only two are on the shelf has two honest outcomes: refuse to
quote the work at all, or quote it and let the shop order the other two. We chose the second.
`PartReservationService.reserveForWorkOrder` claims whatever is available — `Part.reserve` clamps to
`getAvailable()` — records the difference as a shortfall on the `PartReservation` row, and never
throws. The quote goes out with the shortfall attached rather than blocking on it.

Hard-failing the quote instead (throwing when any row is short) was the obvious alternative, and it
was rejected for a plain reason: a shop quotes work it doesn't currently have parts for constantly —
that's the whole reason a purchasing feature exists. If quoting required full stock up front, nothing
in `inventory`'s purchasing surface (mocked vendor calls, reorder rules) would ever get exercised by a
real quote; every shortfall would have to be resolved by hand before a customer ever saw a price.

The physical constraint doesn't disappear, though — it moves to `startService`, which is strict:
`PartReservationService.consumeForWorkOrder` throws `InsufficientStockException` if any reservation
still carries a shortfall, and the whole `@Transactional` `startService` call rolls back, including the
`IN_PROGRESS` status transition. Work never starts on parts that aren't actually on the shelf. That
placement isn't a free choice — it comes with a cost this ADR is recording on purpose: an `APPROVED`
work order holds its reservation indefinitely if the customer never comes back to start the job, since
nothing releases it until `ExpireStaleReservations` ages it out (see ADR 0006) or someone starts
service. Writing off at `approve` instead would make approve/refuse symmetric — no reservation survives
either outcome — but it would decrement `quantityOnHand` before the part physically leaves the shelf,
which is worse for a module whose entire job is answering "what's on the shelf right now?" We took the
stale-reservation cost over the wrong-on-hand cost.

One consequence worth naming: `reserveForWorkOrder` is safe to call more than once for the same work
order — each call adds its own `PartReservation` rows rather than replacing earlier ones. That was a
deliberate choice to keep `finishDiagnostics` simple (it always reserves fresh against the rows it was
just given) rather than needing to diff against a prior quote. It means a work order that gets
requoted multiple times before approval accumulates one reservation batch per quote, all still
resolved together by the same `releaseForWorkOrder`/`consumeForWorkOrder` call. Nothing in the current
flow requotes an approved work order — the state machine has no path back from `WAITING_APPROVAL` — so
this hasn't needed tightening yet.
