# Budget's send/delivery lifecycle stays off WorkOrderStatus

Sending a Budget to a customer means handing it to `email` for async delivery, which can fail and needs
to be retried without redoing the parts of "send" that already happened (locking lines, fixing the
reservation state). That needs at least two sub-states — attempted and confirmed — somewhere.

We put `WAITING_SEND`/`SENT` on `Budget`'s own status, not on `WorkOrderStatus`. `WorkOrderStatus` only
gained one new value for this redesign, `BUDGET_IN_DRAFT`; it jumps straight from that to
`WAITING_APPROVAL`, and only once the Budget is confirmed `SENT`. Outer modules watching
`WorkOrderStatus` never see a send in flight or a delivery retry — that's internal to `workorder` and
its `email` dependency.

The alternative — mirror the same states onto `WorkOrderStatus`
(`BUDGET_IN_DRAFT → BUDGET_WAITING_SEND → BUDGET_SENT → WAITING_APPROVAL`) — was considered and rejected:
it makes email-delivery mechanics a permanent, visible part of the Work Order's top-level status for
every consumer of that enum, for a distinction only the Budget's own resend flow actually needs.
