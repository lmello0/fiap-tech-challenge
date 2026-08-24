# Budget draft line edits reserve/release live, not accumulate per quote

ADR 0007 recorded that `reserveForWorkOrder` was safe to call more than once per work order because
each call added fresh `PartReservation` rows rather than diffing against a prior quote — a deliberate
simplification, since the old state machine had no path back into an editable quote once
`finishDiagnostics` ran once.

The redesigned `Budget` has an explicit editable `DRAFT` phase: a mechanic can add, remove, or resize
lines any number of times before sending. If each edit still called `reserveForWorkOrder` fresh, a
Draft that gets edited ten times would leave nine stale, superseded reservation batches sitting against
the part alongside the one that matters — inventory would see far more claimed than the Budget actually
asks for. So a line add now reserves only that line's delta, and a line removal or quantity decrease
now calls `releaseForWorkOrder` for the corresponding delta, keeping the Draft's live reservation total
equal to its current lines at all times, no more and no less.

This supersedes the accumulation behavior ADR 0007 described (§ "reserveForWorkOrder is safe to call
more than once... each call adds its own PartReservation rows"). Everything else in ADR 0007 still
holds: reservation is still best-effort per line, shortfalls still only hard-block at `startService`,
and `workorder → inventory` is still the only direction of dependency — this ADR only changes how many
times, and with what net effect, `workorder` calls `PartReservationService` during the life of one
Budget.

Shortfall healing (ADR 0019) interacts with this: a Draft's live reservations are just as eligible for
FIFO top-up as an approved work order's, since inventory has no way to tell them apart without reading
budget state. A Draft edited down to a smaller quantity releases its excess immediately (per this ADR),
so healing never tops up more than what the Draft's current lines actually ask for — but a Draft left
open can still absorb stock ahead of an approved order that reserved later.
