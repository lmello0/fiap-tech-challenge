# Reschedule cancels the original Appointment and creates a new one

Moving an Appointment to a new slot could be an in-place update to its date/time, keeping one
continuous row across the change. We rejected that: `SCHEDULED`/`COMPLETED`/`CANCELLED`/`NO_SHOW` are
meant to describe what happened to one specific booked slot, and letting the slot itself mutate would
make that history lie — a `NO_SHOW` Appointment whose date/time had since been edited would no longer
show the slot the customer actually failed to show up for, and the No-Show sweep would have to reason
about an Appointment's *current* slot instead of the one it was actually scheduled against.

Reschedule instead cancels the original (`CANCELLED`, reason `RESCHEDULED`) and creates a new
Appointment carrying the same guest/customer/vehicle/complaint data, linked back via
`rescheduledToId`. The result is one immutable row per booked slot and a walkable chain of what a
visit's booking history looked like, at the cost of an Appointment's id not staying stable across a
reschedule the way a `WorkOrder`'s id stays stable across its whole status lifecycle.
