# Auto Repair Shop

## FIAP - Pos Tech
## Software Architecture
## Tech Challenge - Phase 1

---

Event Storming: [Miro](https://miro.com/app/board/uXjVH6Q299o=/?share_link_id=234351769480)

## Running

- Local dev (Postgres + Mailpit only): `docker compose up`
- Demo pipeline (build, tests, SonarQube analysis): `docker compose --profile sonar up --abort-on-container-exit sonar-analysis`, then check results at http://localhost:9000

