# Auto Repair Shop

## Users & Auth

Identity and account management for the platform: who a person is (`User` and its facets), how they prove it (`auth`), and how access is granted or revoked.

### Language

**User**:
A person registered in the system, identified by email and document. A `User` is a shell that can carry one or both of two facets — `Customer` and `Worker` — at the same time.
_Avoid_: Person, Account (as a synonym for User)

**Customer facet**:
The facet of a `User` that lets them request work orders. Carries its own active/deactivated state, independent of any `Worker` facet the same `User` might also have.
_Avoid_: Client, Buyer

**Worker facet**:
The facet of a `User` that lets them perform work-order tasks (mechanic, attendant, manager). Carries a `role`, hire/start dates, and its own active/terminated state.
_Avoid_: Employee, Staff

**Deactivation**:
Soft-delete of the `Customer` facet, triggered by the customer themselves, an Attendant, or a Manager. Reversible via **Reactivation** (Attendant/Manager only). Does not touch the `Worker` facet of the same `User`, if any.
_Avoid_: Delete, Ban

**Reactivation**:
Restoring a deactivated `Customer` facet to active. Staff-only action.

**Termination**:
Ending a `Worker` facet's employment as of a given date. Distinct from `Deactivation` — termination is an HR-flavored, one-directional event (no reactivation flow), while deactivation is a reversible self-service or staff action on the `Customer` facet.
_Avoid_: Deactivation (when talking about a Worker), Firing

**Account access**:
A `User` may log in as long as at least one of their facets (`Customer` or `Worker`) is active. If every facet the `User` has is deactivated/terminated, login is refused.

**Password Reset Token**:
A single-use, time-limited token proving control of a `User`'s registered email, used to set a new password without knowing the current one (the "magic link" flow). Distinct from an authenticated password change, which proves control by supplying the current password instead.
_Avoid_: Magic token, Reset code

**Email Verification Token**:
A single-use, time-limited token proving control of the email a `User` registered with. Until consumed, the account is unverified and login is refused.

**Email Change Token**:
A single-use, time-limited token proving control of a *new* email address a `User` wants to switch to. The registered email only changes once this token is consumed (double opt-in) — the old email keeps working for login until then.

**Refresh Token**:
An opaque, rotating credential used to obtain new access tokens without re-authenticating. Each use retires the token and issues a successor; presenting an already-retired token is treated as token theft and revokes every session for that `User`.

## Inventory

Parts and services the shop stocks and sells, stock levels with an auditable history, vendor
purchasing (mocked — there is no real vendor API), and the reservations that tie stock to a work
order between quoting and starting the job.

### Language

**Part**:
A physical item held in stock and sold on a work order. Carries a shelf `salePrice` and a derived
`averageCost` computed from purchase receipts.
_Avoid_: Item, Product, SKU (as a synonym for the part itself)

**Service**:
A nameable unit of work the shop sells ("brake pad replacement"). Carries a price and an execution
time that sharpens as the shop performs it. Realized in Java as `RepairService`, because `Service`
collides with Spring's `@Service` stereotype annotation in every class that would need to import both.
_Avoid_: Labor, Job, Task

**Service Execution**:
One measured sample of how long a Service actually took on a work order, captured by a mechanic
starting and finishing that SERVICE row. A Service's quoted execution time is a rolling average over
its most recent samples, not an all-time average — a shop that gets faster isn't anchored to its
first year — and falls back to a seeded estimate until any samples exist.
_Avoid_: Estimate (that's the seed value, not a measured sample)

**Labor**:
Money only — `laborTotal` on a work order is the sum of its SERVICE rows. Never a thing you can point
at or look up; there is no catalog of "labors".
_Avoid_: using "labor" for a catalog entry

**Purchase Order**:
What the shop asks a vendor for, to restock a Part. Never shortened to "order" in prose — `WorkOrder`
is what a customer asks the shop for, and the two must not be conflated.
_Avoid_: Order, PO (in prose)

**Reservation**:
A claim a work order holds on a quantity of a Part between quoting and starting work. Reduces
`available` without moving `quantityOnHand` — a Reservation is not a Stock Movement. Every Reservation
belongs to exactly one work order; there is no way to reserve a part outside of one.
_Avoid_: Hold, Lock (ambiguous with database locking)

**Shortfall**:
The part of a Reservation that could not be satisfied because on-hand stock ran out. Blocks the work
order from starting service and is what triggers reordering.
_Avoid_: Backorder

**Stock Movement**:
An append-only record of a real change in a Part's `quantityOnHand`: `PURCHASE` (a vendor receipt),
`CONSUMPTION` (a work order starting service), or `ADJUSTMENT` (a manual correction against a
physical count, breakage, or loss — always carries a reason). Reservations are not movements.

**Reorder Rule**:
A per-part standing instruction holding a `min` and a `max`: when the part's inventory position
(available stock plus anything already inbound from an open purchase order) falls to or below `min`,
order enough to bring it back up to `max`. A Part with no Reorder Rule is one nobody has decided to
auto-stock; a Reorder Rule that is disabled is a decision someone made.
_Avoid_: Reorder point (names only the trigger, not the order-up-to policy)

**Stockist**:
The `Worker` facet role that runs the storeroom: the part and service catalogs, purchasing, receiving,
reorder rules, and stock adjustments. Holds the same inventory rights as `Manager` — the role exists to
name who is expected to do this work day to day, not to fence `Manager` out of it.
_Avoid_: Stock clerk, Storekeeper

## Work Orders

Repair jobs the shop performs on a customer's vehicle, tracked from intake through diagnostics,
pricing, customer approval, execution, and pickup.

### Language

**Work Order**:
The record of a repair job for one vehicle, tracked through a fixed status lifecycle from intake to
delivery. Holds no line items of its own — the work being done is entirely whatever the current
`Budget`'s lines say, once that Budget is approved.
_Avoid_: Order (collides with `Purchase Order`), Job, Ticket

**Budget**:
A priced proposal of the Parts and Services a Work Order needs, sent to the customer to approve or
refuse. Exactly one Budget is live per Work Order. Once sent, a Budget's lines are frozen — there is no
requoting, only approve or refuse; a refused Budget is terminal for its Work Order.
_Avoid_: Quote (fine in speech, but `Budget` is the canonical term in code and docs), Estimate, Proposal

**Budget Line**:
One priced item on a `Budget`: a `Part` or a `Service` (never both), a quantity, and the price at the
moment it was added. A snapshot taken from the Inventory context's catalog data, not a live reference to
it — the catalog `Part`/`Service` can reprice later without changing what a customer already approved.
_Avoid_: Row, Item (ambiguous with the Inventory `Part`)

**Draft** (Budget status):
The editable phase of a Budget's life, opened the instant diagnostics finish and seeded with the
mechanic's initial lines. Every add, removal, or quantity change while in Draft reserves or releases
stock immediately. Ends the moment staff sends the Budget — a Draft is never partially locked.

**Send** (Budget action):
The staff action that freezes a Draft Budget's lines and hands it to the customer by email. Moves the
Budget to `Waiting Send` while delivery is attempted; only confirmed delivery (`Sent`) moves the owning
Work Order's status to `WAITING_APPROVAL`. A Budget stuck in `Waiting Send` after a delivery failure can
be resent as many times as needed without re-locking or re-reserving anything.
_Avoid_: Submit, Publish

**Diagnostics**:
The mechanic's inspection of the vehicle, which produces the Work Order's first Budget Draft. Distinct
from executing a Budget's lines (the repair itself), which only ever happens after that Budget has been
approved.
