# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Angular (`apps/frontend/`, not yet scaffolded), talking to a Spring Boot API (`apps/backend/`). Backend contract is documented at `backend-openapi.yaml`.

## Users

Two audiences on one platform:

- **Customers and Guests** — vehicle owners. A `Customer` is a registered facet of a `User` who can request work orders, approve/refuse budgets, and manage their vehicles. A `Guest` is an unregistered person who books a drop-off appointment with inline contact/vehicle details, later converted into a real `Customer` via registration or Attendant check-in.
- **Staff** — `Worker` facets of a `User`, distinguished by role: **Attendant** (check-in, guest conversion, booking management), **Mechanic** (diagnostics, budget drafting, service execution), **Manager** (operating calendar, closures, full inventory/HR rights), **Stockist** (part/service catalogs, purchasing, receiving, reorder rules, stock adjustments — same inventory rights as Manager).

## Product Purpose

An auto repair shop management platform: customers/guests book drop-off appointments, staff run diagnostics and propose a priced Budget, the customer approves or refuses it, staff execute the approved work, and the customer picks up the vehicle. Inventory (parts, services, purchasing, stock) and scheduling (appointments, operating calendar, closures) support that core repair-job lifecycle end to end. Success is a shop that can run its full day-to-day operation — intake through delivery — through the platform instead of ad hoc tooling.

## Positioning

Not a generic booking or ticketing tool: work orders, budgets, and inventory reservations are modeled as a tightly linked lifecycle (diagnostics → draft budget → customer approval → reserved stock → execution → pickup), with an immutable, event-sourced History timeline per aggregate and a Guest → Customer conversion path that lets a shop capture walk-in business without forcing registration up front.

## Operating Context

- Staff work from an internal console covering the full operational lifecycle: appointment check-in, diagnostics, budget drafting/sending, inventory/purchasing, reorder rules, and calendar management.
- Customers/Guests interact through a customer-facing experience: booking a drop-off, tracking a work order's status, reviewing and approving/refusing a Budget, booking a pickup, and (once registered) viewing their History timeline and managing vehicles.
- The shop's operating calendar is fixed Monday–Friday 8AM–6PM with Manager-defined closures overriding specific dates; appointments are booked into 30-minute slots with type-specific capacity (drop-off vs. pickup tracked independently).
- Communication with customers/guests (budget delivery, booking invitations, password reset/email verification/change) happens by email; local dev runs this through Mailpit.

## Capabilities and Constraints

- Backend is fully modeled (Users & Auth, Inventory, Work Orders, Scheduling, History — see `apps/backend/CONTEXT.md`) and exposes a REST API (`backend-openapi.yaml`); the frontend is not yet scaffolded, so no visual implementation exists yet to preserve.
- A `User` may hold both a `Customer` and a `Worker` facet simultaneously; the frontend must account for a person who is, e.g., both a shop employee and a customer of the shop.
- Login requires at least one active facet; UI must handle the fully-deactivated/terminated case (login refused) distinctly from ordinary auth failure.
- Budgets are frozen once sent — no requoting UI, only approve/refuse.
- A `Guest` is not backed by a `User` until conversion; UI flows (booking, appointment management) must support the unregistered path via booking-management/complete-registration tokens, not just authenticated sessions.

## Brand Commitments

None yet — no name beyond "Auto Repair Shop" (working title), no logo, palette, or typography committed.

## Evidence on Hand

None. No real content, screenshots, testimonials, or demo data exists yet for the frontend; future work must not fabricate shop branding, customer testimonials, or sample pricing beyond what's needed as clearly-labeled placeholder/demo data.

## Product Principles

1. Model the platform's two audiences (customers/guests and staff-by-role) as genuinely different experiences, not one UI with hidden permissions.
2. Respect the backend's frozen/terminal states (sent Budgets, refused Budgets, terminated Worker facets, No-Show appointments) as real UI states to design for, not edge cases to paper over.
3. Prefer the domain's own vocabulary (`CONTEXT.md`) in UI copy and code — e.g. "Budget" not "Quote", "Work Order" not "Ticket" — since the shop's terms were deliberately chosen to avoid collisions with adjacent concepts.
4. Guest-to-Customer conversion should feel like a natural continuation, not a forced signup wall, since capturing walk-in business without friction is core to the product's positioning.
5. This is an academic project (FIAP Pos Tech, Tech Challenge Phase 1) with no brand assets yet — favor a credible, coherent placeholder identity over generic default styling, but don't over-invest in polish beyond what the challenge requires.

## Accessibility & Inclusion

No specific standard mandated yet; follow ordinary good web accessibility practice (semantic HTML, keyboard operability, sufficient contrast) as a baseline until a requirement is confirmed.
