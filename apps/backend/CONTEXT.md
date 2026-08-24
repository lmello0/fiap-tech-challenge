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
A physical item held in stock and sold on a work order. Carries a shelf `salePrice`; on-hand quantity
and cost are never stored on it — they are derived from Stock Movement (see the `inventory.part_stock`
view).
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
`available` (a derived figure — see Stock Movement) without moving on-hand stock — a Reservation is not
a Stock Movement. Every Reservation belongs to exactly one work order; there is no way to reserve a
part outside of one.
_Avoid_: Hold, Lock (ambiguous with database locking)

**Shortfall**:
The part of a Reservation that could not be satisfied because on-hand stock ran out. Blocks the work
order from starting service. Heals automatically, FIFO by how long a Reservation has been waiting,
whenever stock increases (a Purchase Order receipt or an upward manual adjustment) — a shortfall is
current state the system keeps up to date, not a one-time snapshot from when the Reservation was made.
_Avoid_: Backorder

**Stock Movement**:
The only record of a Part's stock — on-hand is the sum of a Part's Stock Movements, never a stored
number. Three types: `PURCHASE` (a vendor receipt), `CONSUMPTION` (a work order starting service), or
`ADJUSTMENT` (a manual correction against a physical count, breakage, or loss — always carries a
reason). Reservations are not movements.

**Inventory Position**:
What a Stock Policy tests against: available stock plus anything already inbound from an open Purchase
Order. Distinct from `available` alone — a part with a large order already on the way isn't "low" even
if nothing is on the shelf yet.

**Stock Status**:
A Part's derived standing against its Stock Policy, shown on the catalog list: `OUT` (available at or
below zero), `LOW` (available at or below the Stock Policy's `min`), `OK` (above `min`), or `NO_POLICY`
(no Stock Policy has ever been set — nobody has decided what "low" means for this Part). `NO_POLICY` is
deliberately distinct from `OK`: a Part at zero with no policy is the most urgent row on the list, not
an invisible one.

**Stock Policy**:
A per-part standing threshold holding a `min`, and optionally an order-up-to `max` and `vendor` when
its auto-reorder flag is on: when Inventory Position falls to or below `min` and auto-reorder is
enabled, order enough to bring it back up to `max`. With auto-reorder off, a Stock Policy is still
meaningful — it's the threshold a Part's Stock Status is computed against, with no order ever placed
automatically. A Part with no Stock Policy is one nobody has decided to track at all.
_Avoid_: Reorder Rule, Reorder point (names only the auto-order behavior, not the standalone threshold)

**Stockist**:
The `Worker` facet role that runs the storeroom: the part and service catalogs, purchasing, receiving,
stock policies, and stock adjustments. Holds the same inventory rights as `Manager` — the role exists
to name who is expected to do this work day to day, not to fence `Manager` out of it.
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

## Scheduling

Booking a visit to the shop — dropping a vehicle off for diagnostics, and later picking it up once
its Work Order is ready. Drop-offs can be booked by an unregistered Guest as well as a signed-in
Customer; Pickups always belong to a real Customer, since a Work Order can't exist without one.

### Language

**Appointment**:
A booked visit to the shop for a specific 30-minute slot — either a `Drop-off Appointment` or a
`Pickup Appointment`. Auto-confirmed the moment it's booked; there is no separate approval step.
_Avoid_: Schedule (that's the module/bounded context, not a bookable thing), Booking, Reservation
(already means something else — see Inventory's `Reservation`)

**Drop-off Appointment**:
An Appointment to bring a vehicle in for diagnostics. Booked either by a `Guest` (carrying their own
inline contact and vehicle details) or by a signed-in Customer against an existing `Vehicle`.
Checking in a Drop-off Appointment is what produces a `Work Order`.

**Pickup Appointment**:
An Appointment to collect a vehicle once its `Work Order` reaches `WAITING_PICKUP`. Always tied to a
real Customer, Vehicle, and Work Order — never booked by a `Guest`, since reaching `WAITING_PICKUP`
means `Guest Conversion` already happened. Reaching `WAITING_PICKUP` only sends a booking invitation;
no Appointment row exists until someone actually picks a slot.

**Guest**:
An unregistered person booking a Drop-off Appointment — name, phone, email, and vehicle
maker/model/year given directly on the Appointment, not backed by a `User`. Distinct from the
`Customer` facet, which only exists once a real `User` does.
_Avoid_: Customer (when referring to an unregistered person), Anonymous user

**Guest Conversion**:
Turning a Guest's captured details into a real `User` with a `Customer` facet and a `Vehicle`, and
linking the originating Appointment to the new `customerId`/`vehicleId`. Happens one of two ways: the
guest completing registration through their own token link, or an Attendant registering them at
Check-in. A Guest who instead signs up independently through ordinary registration, with a matching
email, is *not* auto-linked — the Appointment stays a guest booking until one of the two explicit
conversion paths happens.
_Avoid_: Registration (Guest Conversion reuses/extends ordinary `User` registration, but is
specifically about linking a prior Appointment to the result)

**Check-in**:
The Attendant action of marking an Appointment `COMPLETED` when the customer or guest physically
arrives for their slot. For a Drop-off, may require `Guest Conversion` first if the guest hasn't
already completed registration on their own. Triggers Work Order creation (Drop-off) or the Work
Order's `DELIVERED` transition (Pickup).

**No-Show**:
Terminal status set automatically, by a scheduled sweep, once a `SCHEDULED` Appointment's slot end
time passes with no Check-in.
_Avoid_: Cancelled (No-Show is distinct from a customer/guest actively cancelling)

**Reschedule**:
Moving an Appointment to a different slot: cancels the original (`CANCELLED`, reason `RESCHEDULED`,
linked via `rescheduledToId`) and creates a new Appointment carrying the same guest/customer/vehicle/
complaint. Changes the slot only — correcting contact details is not something Reschedule does.

**Operating Calendar**:
The Manager-configured rules an Appointment is booked against: the fixed Monday–Friday 8AM–6PM week,
any specific future dates a Manager has closed, and slot capacity (tracked independently per
Appointment type). Not itself a bookable thing.

**Closure**:
A specific future date a Manager marks as not open, overriding the Operating Calendar's default
weekly hours for that date. Closing a date that already has Appointments booked on it cancels them
and notifies the affected Customers/Guests, optionally carrying a Manager-written explanation.
_Avoid_: Holiday (implies a fixed, recurring calendar this doesn't have — every Closure is a one-off
Manager decision)

**Slot Capacity**:
How many Appointments of a given type can be booked into the same slot. Tracked independently per
Appointment type — Drop-off and Pickup never compete for the same capacity.

**Booking-Management Token / Complete-Registration Token**:
Two single-use, hashed, expiring tokens (same shape as the Users & Auth context's tokens — see ADR
0002) sent to a Guest after booking: one lets them view, cancel, or reschedule their Appointment; the
other drives `Guest Conversion`. Kept separate because they authorize different-stakes actions —
managing a booking versus creating login credentials.

## History

An append-only, query-only account of what happened to the things the shop tracks — Work Orders,
Users, Vehicles, and the Inventory catalog. Every entry is written by reacting to a domain event;
nothing writes to it directly, and nothing — including History itself — ever changes or removes what
it has written. It deliberately does not record authentication activity, which is security telemetry
rather than the life of a business entity.

### Language

**History Entry**:
One immutable record of a single thing that happened to one aggregate, carrying when it happened, the
`Actor` behind it, and a `Snapshot`.
_Avoid_: Audit record, Log line, Revision

**Timeline**:
The ordered sequence of `History Entry` rows belonging to one aggregate — the answer to "what happened
to this Work Order". An entry about a `Budget` or a `Budget Line` belongs to its Work Order's Timeline
rather than to one of its own.
_Avoid_: Feed, Trail, Audit log

**Snapshot**:
The state of an aggregate frozen at the moment a `History Entry` was recorded — for a Work Order that
means the order together with its Budgets and their lines; for a `Part`, its catalog fields only, since
`Stock Movement` already accounts for stock levels. Captured by the module that owns the aggregate,
never derived by replaying earlier entries.
_Avoid_: Revision, Version, Diff

**Actor**:
Who or what caused the event behind a `History Entry` — either a `User`, or the system itself when a
scheduled job or a startup task is responsible. "The system did it" and "we do not know who did it" are
different things, and History distinguishes them.
_Avoid_: Author, Owner, Creator

**Customer-visible**:
A property of an event type deciding whether its entries reach a customer's `Timeline`. An event is not
customer-visible unless the module publishing it says so.
