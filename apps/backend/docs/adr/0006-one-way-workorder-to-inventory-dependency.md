# One-way workorder → inventory dependency, reservation called synchronously

Quoting a work order needs to know, right now, whether the parts it's about to promise exist —
`finishDiagnostics` reserves stock, `refuse` releases it, `startService` consumes it. All three sit on
`workorder`, and all three need something only `inventory` owns: the part catalog and stock levels.

The obvious alternative was symmetric with how `email` already works: `inventory` listens for
`WorkOrderApprovedEvent`/`WorkOrderRefusedEvent` and reacts. That doesn't fit here the way it fits
mail. Email delivery genuinely doesn't need to block the request that triggers it — nobody is waiting
on a `EmailRequestedEvent` publish to know if their password reset worked. Reservation is the opposite:
a quote that promises four brake pads when two are on the shelf is meaningfully different from one that
promises four when four exist, and the caller finding that out only after the fact defeats the point of
quoting at all. `PartCatalogService`/`PartReservationService` are called synchronously and can throw
inside the same transaction the quote lives in, so `PartNotFoundException` on a bad part ID rolls back
the whole `finishDiagnostics` call — never a WorkOrder is left half-quoted.

That fixes the direction: `workorder` depends on `inventory.api`, never the reverse. Spring Modulith's
`ApplicationModules.verify()` enforces this as a hard build failure on any cycle, so the constraint
isn't just documented, it's checked on every test run (see `ModularityTests`). This also settles a
question the reservation design otherwise has no good answer to: `inventory` can never ask "is this
work order still alive?" — it has no dependency on `workorder` at all, in either direction. That's why
stale reservations expire on age alone (`ExpireStaleReservations`, 7 days by default) rather than by
checking the work order's status; inventory has no way to check it.

The one exception to "always synchronous" is `inventory` publishing outward —
`PartReservationExpiredEvent`, and later `PartStockLowEvent`/`PurchaseOrderReceivedEvent`. That's the
same direction as the synchronous calls (`workorder` may depend on `inventory.api.events` exactly as it
depends on `inventory.api`), so it costs nothing new; `inventory` still never imports a line from
`workorder`.
