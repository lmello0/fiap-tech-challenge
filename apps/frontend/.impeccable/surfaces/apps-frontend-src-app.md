---
version: 1
slug: "apps-frontend-src-app"
primary_target: "apps/frontend/src/app"
related_targets: ["apps/frontend/src/app/features/work-orders"]
---

# Staff console — surface brief

**Scope:** the whole internal staff console (`apps/frontend`), all four Worker roles.
**Visitor mode:** Operate.

## Audience and job
Four roles at desktop workstations inside a working shop, read in glances between interruptions:
Attendant (counter: check-in, guest conversion, budget send, delivery), Mechanic (bay: diagnostics,
budget lines, per-line execution), Stockist (storeroom: catalogue, purchasing, stock), Manager
(everything, plus calendar, closures, roster). Role gating mirrors the backend's `@PreAuthorize`
exactly — a Mechanic genuinely has no Customers or Vehicles section.

## Task and proof
Read the whole shop's state at a glance; know the next lawful step and who may perform it; perform
it in place. The product truth no neighbouring tool has: a frozen customer-approved Budget is the
sole authority for what gets done, with stock reserved against it from the moment it is drafted.

## Chosen direction
**The Workshop Manual** (seed `f65ca8f3`, index 4 of 7, code-led — no image generation available).
The console as a factory service manual: bleed section tabs, a masthead stamped with the operator's
role as an authority line, and every work order as a numbered row carrying its 11-status lifecycle
as a **step rail**. WARNING / CAUTION / NOTE are the three severity tiers, mapped onto real domain
rules (shortfall blocks; sending freezes; refusal is terminal). Light ground, forced by the scene:
fluorescent light and daylight through a roller door.

**Memorable moment:** the step rail. Hovering or focusing any tick states that step's precondition
and the roles the API will accept — the permission model becomes readable instead of hidden.

## Constraints
- Desktop only, 1280 minimum, 1440–1920 target. No mobile layouts by user decision.
- Frontend-only build; designed against enriched DTOs specified in `docs/backend-requirements.md`,
  read through adapters so it degrades rather than breaks against today's API.
- Synthetic demo data, labelled on-screen; replace wholesale when the API is reachable.
- Angular 22, zoneless, signals, standalone, lazy routes.

## Depth
Deep: the shell and the Work Order lifecycle (board + detail, budget editing, history).
Mapped at list level: Scheduling, Inventory, Customers, Vehicles, Workers.

## Unresolved
- Mechanic scoped read of customer/vehicle on their own assigned orders (backend decision).
- i18n: UI is English, currency BRL, dates en-GB. No localisation decision recorded.
- Auth flows (login, forced password change, refresh rotation) are modelled in `Session` but no
  login screen is built — the console assumes an authenticated worker.
