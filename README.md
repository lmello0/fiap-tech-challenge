# Auto Repair Shop

## FIAP - Pos Tech
## Software Architecture
## Tech Challenge - Phase 1

---

Event Storming: [Miro](https://miro.com/app/board/uXjVH6Q299o=/?share_link_id=234351769480)

## Layout

Monorepo. See [CONTEXT-MAP.md](CONTEXT-MAP.md) for the domain model.

- `apps/backend/` — the Spring Boot API. See `apps/backend/HELP.md` for Maven/Spring Boot basics.
- `apps/frontend/` — the Angular staff console. See `apps/frontend/README.md`.

## Running

- Local dev (Postgres + Mailpit only): `docker compose up`
- Frontend dev server: `docker compose --profile frontend up`, or `npm start` in `apps/frontend/`
- Demo pipeline (build, tests, SonarQube analysis): `docker compose --profile sonar up --abort-on-container-exit sonar-analysis`, then check results at http://localhost:9000

