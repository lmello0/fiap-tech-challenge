# Appointment ↔ Work Order coordination is event-driven, unlike WorkOrder → Inventory

ADR 0006 chose a synchronous call for `workorder` → `inventory` reservation specifically because a
quote that promises parts it can't deliver is worse than one that fails inline before the customer
ever sees it. `scheduling` → `workorder` looks superficially similar — Check-in needs to produce a
`WorkOrder`, and later a `WorkOrder` reaching `WAITING_PICKUP` needs to prompt a Pickup booking — but
the failure mode isn't the same. The customer is already physically standing at the counter whether
`WorkOrder` creation completes in the same request or a few seconds later; there's no promise being
made to them that reservation-style urgency protects. Unlike inventory's stock levels, nothing about
a completed Check-in becomes wrong by waiting.

So the handoff is `@ApplicationModuleListener` events, not a synchronous call — and, like ADR 0006,
still strictly one-way: `workorder` depends on `scheduling.api.events`, `scheduling` depends on
nothing of `workorder`'s. `ApplicationModules.verify()` doesn't grant events an exception from cycle
detection — a bidirectional dependency is a violation whether it's built from service calls or event
types, so both hand-offs had to point the same direction. `scheduling` publishes
`AppointmentCheckedInEvent` on Check-in; `workorder` listens and, on `DROPOFF`, creates the
`WorkOrder`, or on `PICKUP`, transitions it to `DELIVERED` — using the `workOrderId` the event already
carries, no call back into `scheduling` needed. The reverse case — `workorder` reaching
`WAITING_PICKUP` needs to prompt a Pickup booking — can't be `workorder` depending on a `scheduling`
listener the usual way, since `scheduling` has nothing to react to yet; instead a listener inside
`workorder` catches its own `WorkOrderWaitingPickupEvent` and republishes it as
`scheduling.api.events.PickupInvitationRequestedEvent` — a type `scheduling` owns and listens to
itself, the same shape as every module depending on `email.api.EmailRequestedEvent`. The dependency
this creates still only points from `workorder` to `scheduling`.

The cost is that Attendant doesn't get ADR 0006's inline failure signal: a listener failure (e.g. an
unexpected data problem creating the `WorkOrder`) leaves the Appointment `COMPLETED` with no
`WorkOrder` yet, surfaced through the same retry/staleness machinery `email` already relies on (ADR
0004), not a rolled-back Check-in.
