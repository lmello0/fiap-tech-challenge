---
version: 1
slug: "apps-frontend-src-app-features-my"
primary_target: "apps/frontend/src/app/features/my"
related_targets: ["apps/frontend/src/app/features/landing","apps/frontend/src/app/features/register","apps/frontend/src/app/features/choose-facet","apps/frontend/src/app/features/sign-in"]
---

# Customer facet — surface brief

**Scope:** the public front door (`/`, `/register`, `/sign-in`, `/choose`) and the whole customer
console (`/my/**`).
**Visitor mode:** Persuade on the landing; Operate everywhere behind it.

## Audience and job
Vehicle owners, most of them on a phone, most of them standing near the car. Two jobs, and they are
not equally frequent: get the car booked in (once), and answer the one question that stops the shop
dead (whenever the shop asks) — *do you approve this price?* A third audience shares the door: a
`User` holding both a Customer and a Worker facet, who must pick a volume and be able to switch
back without signing out.

## Task and proof
The product truth no neighbouring booking tool has: a sent Budget is **frozen** — the shop cannot
add to it after the fact — and refusing it is **terminal**, never quietly requoted. Every customer
surface is built to make those two facts unmissable before the decision, not after it. The landing
proves it by printing the whole ten-step procedure up front, including the two steps that stop
until the customer acts.

## Chosen direction
**The Job Card** (seed `4bd2e1c2`, dealt indices 7/2/4, index 2 locked by the user against The
Service Log and The Thumb Index; code-led — no image generation available).

The public surfaces are the shop's loose stationery: a form-header band, blanks that are ruled
lines rather than boxes, a filing band, and a carbon-copy routing margin. The console behind them
is *Volume 2 — Owner's Manual*, the staff console's shell rebuilt for a reader who is not at a
desk. Both inherit the Shop Manual world unchanged; only the artifact differs.

**Memorable moment:** the routing margin on the landing. A stranger sees all ten steps, in order,
with the two that wait on them flagged, before filling a single blank.

## Constraints
- **Adapts, unlike the console.** Two structural breakpoints (62rem, 46rem). Desktop-only was a
  Volume 1 decision and does not extend to a public door.
- The API gives a customer **no list of their own work orders or appointments** (filed as items 11
  and 12 in `docs/backend-requirements.md`). The console keeps a per-user `localStorage` index of
  what this device has been shown and says so on every screen that reads it.
- `CustomerWorkOrderView` carries four fields. The console shows those four and never reconstructs
  the mechanic or the diagnosis the API withholds on purpose.
- Login is refused for any unverified email, including a customer who just registered and holds a
  working token. Registration says so; sign-in turns that 403 into a resend action.
- The facet is sticky per session (`sessionStorage`) and switchable from either masthead. Leaving
  the Worker facet drops `ShopStore` entirely.

## Depth
Deep: the landing job card, registration, the facet picker, and the job detail with its budget
decision. Full but plainer: vehicles, bookings, profile.

## Unresolved
- Guest booking is live on the landing (`POST /appointments/dropoff/guest`), but the guest-token
  management flows (`/appointments/guest/view|cancel|reschedule`, `guest/complete-registration`)
  have no screens yet — a guest manages a booking only through the shop's email.
- Pickup booking is reachable from a `WAITING_PICKUP` job; there is no standalone pickup screen.
- i18n: UI is English, currency BRL, dates en-GB. Native date inputs follow the browser locale.
