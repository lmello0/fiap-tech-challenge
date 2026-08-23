# Backend requirements for the staff console

What the Angular staff console needs from the API that the API does not provide yet.

The console is **already built against these shapes**. Every one of them is optional at the
TypeScript level and read through an adapter, so the UI renders correctly against the current
contract too — it just renders less. Each item below says what degrades while it is missing.

Ordered by how much the console depends on it.

---

## 1. Shortfall read endpoint — blocking, and not merely enrichment

**Status: the capability does not exist in the REST surface at all.**

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

**While missing:** the adapter returns `blocked: false` with an empty list, so the board shows
*nothing* rather than falsely claiming everything is clear. The reactive path — a 409 from
`service/start` — stays the authority, and the console surfaces the error on the failed attempt.

Frontend contract: `WorkOrderBlock` / `Shortfall` in `src/app/core/domain/models.ts`;
adapter seam is `ShopStore.blockFor()`.

---

## 2. `WorkOrderInfo` — resolve the identity UUIDs

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

## 3. `AppointmentInfo` — same treatment

Add `customerName`, `vehicleLabel`, `vehiclePlate`, and `workOrderCode` (for pickups).

Guest bookings already carry their details inline (`guestName`, `guestVehicleMake`, …) and need
nothing further — the gap is only on the registered-customer path.

**While missing:** the schedule reads correctly for guests and poorly for customers, which is
exactly backwards from how a counter is actually worked.

---

## 4. `StockMovementInfo.referenceId` — resolve the reference

`referenceId` points at either a purchase order or a work order with no way to tell which, and no
human-readable handle.

**Add:** `referenceLabel` (e.g. `"PO-2026-0146"` or `"WO-2026-0731"`), and ideally
`referenceType` as an enum.

**While missing:** the movements ledger cannot say what a consumption was consumed *for* — the
single most useful column in a stock audit.

---

## 5. `WorkOrderFilterQuery` — multi-status and free-text search

Currently `status` accepts one value. A shop status board is almost never filtered to exactly one
step; the useful filters are "everything waiting on a customer" or "everything blocked".

**Add:**
- `statuses: List<WorkOrderStatus>` (keep `status` for compatibility)
- `search: String` matching across customer name and licence plate

**While missing:** the console filters client-side over the current page, which is correct only
while the whole board fits in one page. It will silently under-report once a shop passes a few
hundred live orders.

---

## 6. Aggregate status counts

The board prints a count per lifecycle step. Deriving those from one page of results is wrong at
any real volume.

**Proposed:** `GET /work-orders/summary` → `{ "RECEIVED": 4, "WAITING_DIAGNOSTICS": 2, … }`

**While missing:** counts are derived from the loaded page and are accurate only for small shops.

---

## 7. Open question — Mechanic visibility of customer and vehicle

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
