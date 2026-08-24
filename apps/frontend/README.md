# Frontend

The Angular staff console for the Auto Repair Shop — work orders, scheduling, inventory,
customers, vehicles and the worker roster, for all four `Worker` roles.

Angular 22, zoneless, signals, standalone components, lazy feature routes.

## Running

```sh
npm install
npm start          # http://localhost:4200
```

The backend's `APP_BASE_URL` already defaults to `http://localhost:4200` — see
`apps/backend/.env.example`. `docker compose --profile app up` (from the repo root) runs it via the
`frontend` service in the root `compose.yaml`, alongside the backend.

## Demo data

The console currently runs on **synthetic in-memory data** (`src/app/core/data/demo-data.ts`),
labelled as such in a band across the top of every screen. Nothing in it is real shop data. A role
switcher in that band walks the console as each of the four roles without a running backend.

`ShopStore` (`src/app/core/data/shop-store.ts`) is the seam: it applies the same rules the backend
applies — a sent budget freezes, a draft edit moves a reservation, starting service consumes stock
and writes a movement. Swapping it for HTTP calls does not change the component layer.

## Backend contract

`backend-openapi.yaml` is the current API contract.

The console is built against a slightly **enriched** contract — list rows carry customer name,
plate and mechanic name rather than bare UUIDs, and shortfall is readable before a mechanic
attempts to start service. Those additions are specified in
[`docs/backend-requirements.md`](docs/backend-requirements.md), ordered by dependency, each noting
what degrades while it is missing. Every enriched field is optional and read through an adapter,
so the console renders correctly against today's API too.

## Design

The console's visual system and its rules are recorded in `DESIGN.md`. The direction contract sits
in the body of `src/index.html` and survives the production build.

The spine of the design is `src/app/core/domain/lifecycle.ts` — the eleven work order statuses
written out as a numbered procedure with preconditions and the roles the API will accept for each
step. It is transcribed from `WorkOrderStatus` and the backend's `@PreAuthorize` annotations. The
step rail, the row actions and the permission notices all read from it, so changing a backend rule
means changing that one file.
