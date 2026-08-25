# Backend requirements for the frontend

What the Angular staff console needs from the API that the API does not provide yet.

**The console is now wired to the live API** (`http://localhost:8080`, CORS-allowed for
`http://localhost:4200`). Every read and every lifecycle action below goes to a real endpoint;
nothing in live mode is stubbed. The wire types in `src/app/core/api/dto.ts` are transcribed
one-for-one from the Java `record`s under `**/api/representation/`, and `core/data/mappers.ts`
is the only thing that crosses between wire and domain.

Each item below says what degrades while it is missing.

Items 1–10 are the staff console's. Items 11–13 came out of building the **customer facet** and are
the only ones still open with no workaround that survives a change of device.

> **Status of this document — updated after wiring.**
> Item 1 has **landed** and is consumed. Items 2–4 were closed a different way: rather than the
> API enriching its DTOs, the console resolves ids client-side (`core/data/enrich.ts`), bulk-first
> with a per-id fallback. That is the agreed N+1 and it is good enough at shop scale; the items
> are kept because the server-side join is still the better answer if list sizes grow.

---

## 1. Shortfall read endpoint — ✅ **landed and consumed**

**Status: shipped as `GET /work-orders/{workOrderId}/blocking-shortfalls`.**

The endpoint returns a bare array of `BlockingShortfallInfo`
(`partId`, `partSku`, `partName`, `quantityShort`) and is guarded by
`hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')`.

The console calls it for every `APPROVED` work order — the only status with stock consumption as
its next step — and joins `available` from the `/parts/stock` standing it already holds, so the
board shows "need N, have M, short K" without a second round trip per part.

Two notes for whoever revisits this:

- **An `ATTENDANT` is refused (403).** The store reads that as *unknown*, not as *clear*: the row
  simply carries no shortfall band. Widening the role to match the work order read would let an
  attendant see why a job is stuck, which is arguably their business.
- **The batch form is still worth having.** The board currently issues one request per approved
  order. `GET /work-orders/blocking-shortfalls?ids=…` would collapse that to one.

The original proposal, kept for reference:

<details>
<summary>Original request (superseded)</summary>

Shortfall is modelled internally (`PartReservation`, `InsufficientStockException`,
`PartReservationService`) but is never projected onto a representation. Today the only way to
discover that a work order is blocked is to attempt `POST /work-orders/{id}/service/start` and
catch the exception.

That makes the shop status board's headline behaviour — showing which jobs are blocked and why,
*before* anyone tries to start them — impossible to render.

**Proposed:**

```
GET /work-orders/{id}/blocking
GET /work-orders/blocking?ids=<uuid>,<uuid>,…     # batch, for the board
```

```jsonc
{
  "workOrderId": "…",
  "blocked": true,
  "shortfalls": [
    {
      "partId": "…",
      "sku": "ENG-BLT-0173",
      "partName": "Timing belt kit",
      "required": 1,
      "available": 0,
      "short": 1,
      "unitOfMeasure": "SET",
      "inboundPurchaseOrderCode": "PO-2026-0148",  // nullable
      "inboundEta": "2026-08-24T13:00:00Z"          // nullable
    }
  ]
}
```

Roles: same as the work order read — `ATTENDANT`, `MECHANIC`, `MANAGER`.

</details>

`inboundPurchaseOrderCode` / `inboundEta` were **not** shipped, so the board no longer claims
anything about inbound cover. Frontend contract: `WorkOrderBlock` / `Shortfall` in
`src/app/core/domain/models.ts`; consumed by `ShopStore.refreshBlocks()` / `blockFor()`.

---

## 2. `WorkOrderInfo` — resolve the identity UUIDs — *worked around, still wanted*

> **Now handled client-side.** `core/data/enrich.ts` bulk-loads `/customers` and `/vehicles`
> (one page each) and resolves anything left over per id, caching for the session and collapsing
> concurrent callers onto one request. A mechanic is resolved through `GET /users/{id}` — which
> any staff principal may call — rather than `/workers/{id}`, which is `MANAGER`-only.
> This is fine at shop scale. It stops being fine when the shop outgrows one page of each list.


`WorkOrderInfo` returns `customerId`, `vehicleId` and `assignedMechanicId` as bare UUIDs. A board
row cannot show *who* and *which car* without an N+1 fan-out per row.

**Add:**

| Field | Type | Source |
|---|---|---|
| `customerName` | string | `User.name` via the customer facet |
| `vehicleLabel` | string | `"{make} {model} {modelYear}"` |
| `vehiclePlate` | string | `Vehicle.licensePlate` |
| `assignedMechanicName` | string, nullable | `User.name` via the worker facet |

A separate `WorkOrderListItem` projection would be equally acceptable — the console does not care
whether the detail DTO grows or a list DTO appears, only that a row is self-describing.

**While missing:** rows fall back to order code, status and timestamps only; the customer and
vehicle columns render em-dashes. Scanability drops sharply — this is the difference between a
board a service advisor can read across and one they have to click through.

---

## 3. `AppointmentInfo` — same treatment — *same workaround applies*

Add `customerName`, `vehicleLabel`, `vehiclePlate`, and `workOrderCode` (for pickups).

Guest bookings already carry their details inline (`guestName`, `guestVehicleMake`, …) and need
nothing further — the gap is only on the registered-customer path.

**While missing:** the schedule reads correctly for guests and poorly for customers, which is
exactly backwards from how a counter is actually worked.

---

## 4. `StockMovementInfo.referenceId` — resolve the reference — *still open*

> Unresolved: a movement's `referenceId` is a bare UUID that may point at a purchase order **or**
> a work order, and nothing on the wire says which. The console cannot label it without guessing,
> so it prints the raw reference. This is the one enrichment gap with no client-side workaround.


`referenceId` points at either a purchase order or a work order with no way to tell which, and no
human-readable handle.

**Add:** `referenceLabel` (e.g. `"PO-2026-0146"` or `"WO-2026-0731"`), and ideally
`referenceType` as an enum.

**While missing:** the movements ledger cannot say what a consumption was consumed *for* — the
single most useful column in a stock audit.

---

## 5. `WorkOrderFilterQuery` — ✅ **landed and consumed**

The board sends its whole query to the API and narrows nothing itself. Filters are **ANDed**, so one
free-text box cannot fan out across several columns — it would return nothing. The board therefore
names the column it is looking up ("look up by Plate / Customer / Make / Model / Order code /
Mechanic"), which is how a printed index is used, and the placeholder shows an example value rather
than repeating the label.

**Verified against the running server, 24 Aug 2026** — each field sent on its own and its answer
compared with the unfiltered result:

| Field | State |
|---|---|
| `status` | ✅ single and multiple values |
| `code` | ✅ case-insensitive substring |
| `customerName` | ✅ case-insensitive substring |
| `vehiclePlate` | ✅ case-insensitive substring |
| `vehicleMake` | ✅ case-insensitive substring |
| `vehicleModel` | ✅ case-insensitive substring |
| `mechanicName` | ✅ case-insensitive substring |

Punctuation is stripped from a plate term before sending: plates are stored bare, and an operator
reading one off a document may type the separator printed on it. Nothing else is transformed.

Two consequences worth knowing:

- **The detail view loads its own order** (`ShopStore.ensureWorkOrder`). It is reachable by link, so
  it cannot assume the board's current query happens to include it.
- **Demo mode applies the same narrowing locally**, since it has no API behind it. The two paths
  agree on what each filter means.

---

## 6. Aggregate status counts — ✅ **landed and consumed**

The board's step filter prints the database's own counts rather than counting the rows it happens to
hold, so they stay right at any volume. Called unbounded; `start`/`end` are genuinely optional and a
window correctly narrows the counts.

`ShopStore.statusCounts` still falls back to counting the loaded page if the endpoint is ever
refused, and the board says so in its sub-line ("step counts are of loaded rows") so nobody reads an
approximate count as exact. That failure is deliberately **not** raised on the API fault band, unlike
every other read: there is a correct fallback, and alarming the operator about something they cannot
act on and that costs them nothing would be noise.

## 7. Open question — Mechanic visibility of customer and vehicle — ✅ **answered**

> **Resolved in the current code.** `CustomerController.list/getById` is annotated
> `hasAnyRole('ATTENDANT', 'MANAGER', 'MECHANIC')`, so a mechanic *can* read customers, and
> `GET /users/{id}` is open to any `WORKER`. The console's nav still omits the Customers and
> Vehicles sections for a mechanic — those remain attendant/manager sections by design — but a
> mechanic's board now names the customer and vehicle on their own jobs, which was the actual need.


`CustomerController` and `VehicleController` admit `ATTENDANT` and `MANAGER` but **not**
`MECHANIC`. A mechanic therefore cannot resolve the owner or the vehicle record of a work order
they are personally assigned to.

The console honours this today: a mechanic sees the complaint, the diagnosis and the budget lines,
but no customer record, and the Customers and Vehicles sections are absent from their tab index.

This may well be deliberate — a mechanic arguably has no business browsing the customer list. But
scoped read of the customer and vehicle *on their own assigned work orders* is a different
question from browsing, and the console would use it if it existed.

**Decision needed:** leave as is, or add a scoped read on the assigned work order.

---

## 8. Two lifecycle steps the console could not perform — ✅ **both closed**

### 8a. `POST /work-orders/{id}/diagnostics/finish` — ✅ **form built**

The detail view now carries a diagnosis band that collects the written diagnosis and seeds the
opening budget lines, showing each part's shelf position (`10 available → 9 after reserving`)
against the line that causes it. One call records the diagnosis and drafts the budget.

### 8b. `POST /work-orders/{id}/diagnostics/start` — ✅ **mechanic picker built**

Starting diagnostics now opens a band with a mechanic picker. `/workers` is `MANAGER`-only, so a
non-manager sees only themselves in the list and the band says why — that is the role matrix, not
a gap.

**The whole lifecycle is now performable from the console**, verified end to end against the live
API: intake → diagnostics → diagnosis and budget → send → *(customer approves)* → start service →
finish → ready for pickup → delivered. Step 6 is the customer's alone and correctly has no staff
action.

---

## 9. Records — ✅ **create, update and deactivate built**

The console was read-and-lifecycle only; signed in as a manager against an empty database there was
nothing you could do. It now writes:

| Section | Create | Update | Deactivate |
|---|---|---|---|
| Customers | `POST /customers` | `PATCH /customers/{id}` | soft-delete + reactivate |
| Vehicles | `POST /vehicles` | `PATCH /vehicles/{id}` | soft-delete |
| Parts | `POST /parts` | `PATCH /parts/{id}` | withdraw |
| Stock | `POST /parts/{id}/stock/adjustments` | — | — |
| Labour | `POST /services` | `PATCH /services/{id}` | withdraw |
| Work orders | `POST /work-orders` | lifecycle only | — |

Editing happens in a **ruled entry band**: the row unfolds in place on the same grid, and creating
opens the same band as a blank line at the head of the register. There are no dialogs anywhere,
including for confirmations — the visual world forbids floating panels.

Pass 2 added the rest:

| Section | Create | Update | Remove |
|---|---|---|---|
| Vendors | `POST /vendors` | `PATCH /vendors/{id}` | deactivate |
| Purchase orders | `POST /purchase-orders` | receipts (`/receipts`) | cancel |
| Reorder thresholds | `POST /stock-policies` | `PATCH /stock-policies/{id}` | **delete** (a rule, not a record) |
| Workers | `POST /auth/register/worker` | `PATCH /workers/{id}` | terminate |
| Shop hours | — | `PUT /scheduling/settings` | — |
| Closures | `POST /scheduling/closures` | — | `DELETE .../{date}` |
| Appointments | `POST /appointments/dropoff/on-behalf` | reschedule | cancel |

Notes on two of them:

- **A reorder threshold opens on the part's own row**, not in a separate register — it belongs to
  the part, and the band states the part's current availability so the operator can see whether the
  threshold they are setting fires immediately.
- **Receiving is per line**, pre-filled with what is still owed at the price it was ordered at, and
  says plainly that the unit cost entered moves the part's moving-average cost. Partial receipts
  settle the order at `PARTIALLY_RECEIVED` and stay receivable.

The whole console is now writable. Nothing in it is stubbed.

### Open backend items found while building this

0. **`guestEmail` on `POST /appointments/dropoff/on-behalf` is load-bearing but declared optional.**
   The XOR guard (customer *or* guest details, not both, not neither) works correctly and answers a
   clean 400 either way. Inside the guest branch, though, `guestEmail` specifically is required:

   | Payload | Result |
   |---|---|
   | `guestEmail` only | ✅ 201 |
   | `guestName` + `guestPhone`, no email | ❌ **500** |
   | customer *and* guest details | ✅ 400, with your message |
   | neither | ✅ 400, with your message |

   The booking-management token is emailed to the guest, so a booking with nowhere to send it fails
   downstream rather than being rejected up front. It wants `@NotBlank` on `guestEmail` (or an
   explicit 400). The console requires it in the form and says why, so an operator never reaches
   the 500.

1. **`actorLabel` carries an authority, not a name.** History entries come back with
   `actorLabel: "ROLE_CUSTOMER"` for `USER` actors and a class name (`WorkOrderWaitingPickup`) for
   `SYSTEM` ones. Neither identifies a person, so the timeline renders "Recorded by a customer"
   rather than putting a constant where a name belongs. A user's display name here would make the
   History plate genuinely useful. Related: for a dual-facet user the resolver picks the customer
   facet even when the person is acting as staff, so staff actions are attributed to the customer.
2. **A batch shortfall read** (item 1) would still collapse the board's per-order requests.

---

## 10. Seed data — the database is empty

Not a contract gap, but it blocks anyone reviewing the console against live data. A fresh
database contains only the bootstrap `MANAGER` and nothing else: no work orders, parts, services,
vendors, customers or vehicles. Every live screen therefore renders its empty state, correctly but
uninformatively.

The console ships a **demo mode** (the button on the sign-in page) that reads a synthetic shop from
`core/data/demo-data.ts` so the screens can be reviewed regardless. It is announced in a standing
band, signs in to nothing and calls no API — it is never entered automatically, and never
substituted for a failed request.

A seeding routine, or a Flyway migration behind a `dev` profile, would remove the need for it.

---

---

## 11. A customer's own work orders — ⛔ **missing, and the customer console works around it**

**There is no way for a `CUSTOMER` principal to list their own work orders.**

`GET /work-orders` is `hasAnyRole('ATTENDANT', 'MECHANIC', 'MANAGER')`. The entire customer-facing
work order surface is addressed by id — `GET /work-orders/{id}/customer-view`,
`.../budget/approval`, `.../budget/refusal`, `.../history` — and a customer has no call that
hands them those ids in the first place.

So the one screen the whole lifecycle waits on, the budget decision, is reachable only by
following the link in the shop's email. A customer who deletes that email, or opens the console
on a second device, has no route back to their own job.

**What the console does instead.** `CustomerStore` keeps a `localStorage` index, scoped to the
signed-in user, of every work order id this browser has been shown, and re-reads each one from
`/customer-view` on load. Section 2.2 of the owner's manual lets a reference be added by hand.
Both screens say plainly that this is a record of what the device has seen and not a statement
about the account — the limitation is disclosed to the customer rather than hidden.

**Proposed:**

```
GET /work-orders/mine?status=…          # paged CustomerWorkOrderView, scoped from the token
```

Scoped from `authentication.getName()` exactly the way `GET /vehicles` already scopes itself for a
customer caller, so there is a working precedent in the same codebase. `CustomerWorkOrderView` is
already the right representation and already exists.

**What degrades while it is missing:** a customer cannot find a job they were not emailed a link
to, and the local index does not follow them between devices or survive clearing site data.

---

## 12. A customer's own appointments — ⛔ **missing, same shape**

`GET /appointments` and `GET /appointments/{id}` are both `hasAnyRole('ATTENDANT', 'MANAGER')`,
while `POST /appointments/{id}/customer-cancel` and `.../customer-reschedule` are
`hasRole('CUSTOMER')`. A customer may therefore *change* a booking they can never *read*.

**What the console does instead.** The booking's full `AppointmentInfo` is stored locally at the
moment it is made, because there is no endpoint to read one back. Section 3.3 says so.

**Proposed:**

```
GET /appointments/mine?status=…         # paged AppointmentInfo, scoped from the token
```

**What degrades while it is missing:** a booking made on one device cannot be seen, moved or
cancelled from another, and the shop's confirmation email is the only durable record the customer
has.

---

## 13. `POST /auth/register/customer` returns a token the account cannot yet re-use

Not a missing endpoint — a sequencing surprise worth writing down.

Registration answers with a full token pair, so a new customer is signed in immediately. But
`AuthServiceImpl.login` refuses **every** account whose email is unverified, so that same customer
cannot sign in again until they open the confirmation link. The first session works; the second is
refused with a 403 the customer has no obvious way to read.

The console handles it: registration says the address must be confirmed before signing in again,
and the sign-in screen turns that specific 403 into its own state with a "send the link again"
action rather than a generic failure that would send someone to reset a password that is fine.

Worth deciding deliberately on the backend side: either registration should not issue a session
for an unverified address, or `login` should let an unverified account through in a reduced state.
The current pair is defensible but surprising.

**Guest conversion no longer has this problem** — see item 15, which is the same argument applied
where the proof already exists.

---

## 14. Slot instants are anchored to UTC, not to the shop's timezone

Found while verifying the landing page's booking flow against the running stack.

`GET /appointments/availability` returns the shop's 8am–6pm operating window as
`2026-08-25T08:00:00Z … 2026-08-25T17:30:00Z`. The backend container has no `TZ` set, so the
`LocalTime` business hours are anchored to UTC. A customer in `America/Sao_Paulo` — which is
everyone this shop serves — sees those instants rendered in their own zone as **05:00–14:30**,
directly under a line that says the shop is open 8am to 6pm.

The frontend renders the instant correctly and deliberately does not compensate. Hardcoding a
display zone would produce the right numbers today and the wrong ones the moment the backend is
configured properly, and it would lie to anyone reading the page from another zone.

**Proposed:** set the shop's zone explicitly rather than inheriting the container's. Either
`TZ=America/Sao_Paulo` on the backend service in `compose.yaml`, or — better, because it survives
a move to a differently-configured host — an explicit shop timezone property that
`AppointmentService` uses when it turns `businessStartTime`/`businessEndTime` into instants.

**What degrades while it is missing:** every slot the customer is offered, on the public landing
page and in the owner's manual, is shown three hours earlier than the shop is open.

---

## 15. Guest conversion marks the email verified — ✅ **changed, one line**

`GuestConversionService.completeRegistrationViaToken` now calls
`userService.markEmailVerified(user.id())` after `registerCustomer`.

**Why.** Self-service conversion consumes a *single-use token that was mailed to that exact
address*. Control of the inbox is therefore already proven, which is the only thing email
verification establishes. Without the line the flow stranded the customer completely: the shared
`registerCustomer` path leaves the address unverified, `login` refuses every unverified account
(item 13), and conversion returns no session of its own — so a customer who had just acted on one
email would have had to go and find a second one.

The frontend's `/appointments/complete-registration` success screen depends on this being true: it
says there is no confirmation email to chase and sends them straight to sign in. Reverting the
line would make that screen a lie, so the two move together.

**Still open, deliberately:** `GuestConversionResult` carries no token, so the customer signs in
rather than landing authenticated. Returning a session would mean changing that record and its
controller. One sign-in with a password the customer chose thirty seconds earlier is a fine
ending, and the smaller change is the better trade.

## Not requested

For the avoidance of doubt, the console does **not** need any of these, because the domain already
answers them well:

- `BudgetLineInfo.description` already carries a human label — no catalogue lookup needed to render
  a budget.
- `PartInfo` already exposes `quantityOnHand`, `quantityReserved` and `available` separately, which
  is exactly what the reserve-versus-consume distinction needs.
- The lifecycle and its role gating are fully derivable from `WorkOrderStatus` plus the
  `@PreAuthorize` annotations; they are transcribed in
  `src/app/core/domain/lifecycle.ts` and need no endpoint.
- A customer-facing *reading* of that lifecycle is a copy decision, not an endpoint:
  `src/app/core/domain/customer-procedure.ts` derives it from the same file.
- `GET /vehicles` already scopes itself to the caller for a `CUSTOMER` principal, so the owner's
  manual needs no separate "my vehicles" call.

---
