# Budget is its own entity; Work Order holds no line items

The original `workorder` design put rows directly on `WorkOrder` — one mutable list, with
`laborTotal`/`partsTotal`/`grandTotal` computed over it. "The budget" was never a real thing, just that
list at the moment `WAITING_APPROVAL` was entered. A refusal followed by a re-quote left no trace of
what was actually shown to (and refused by) the customer.

We split `Budget` out as its own entity, owned by the Work Order, with its own status
(`DRAFT → WAITING_SEND → SENT → {APPROVED|REFUSED}`) and its own lines. The Work Order itself now holds
no items at all — once a Budget is `APPROVED`, its lines *are* the work; mechanics execute directly
against them. This is a real behavior change, not just a rename: a Work Order can no longer have "some
items" independent of a priced, customer-facing proposal. Every line that ever gets worked on passed
through a Budget that was sent to, and approved by, the customer.

The alternative — keep one evolving list on `WorkOrder`, treat `Budget` as a read-only projection of it
at approval time — was rejected because it can't answer "what did we actually quote them" once the list
keeps changing after that point, which is exactly the gap that prompted this redesign.
