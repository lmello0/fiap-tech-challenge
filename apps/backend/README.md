# Backend

The Auto Repair Shop's Spring Boot API — auth, users, vehicles, work orders (diagnostics →
budget → execution), inventory/purchasing and scheduling, with an event-sourced History timeline.
See `CONTEXT.md` for the domain model and `backend-openapi.yaml` (repo root) for the API contract.

Java, Spring Boot, Spring Modulith (module boundaries under `src/main/java/.../techchallenge/`:
`auth`, `user`, `vehicle`, `workorder`, `inventory`, `scheduling`, `history`, `email`, `shared`),
Spring Data JPA + Flyway on Postgres, Spring Security with OAuth2 resource server (JWT).

## Vulnerability check

To run vulnerability check (dependency-check plugin on `pom.xml`) is necessary to generate an API Token on [NIST official website](https://nvd.nist.gov/developers/request-an-api-key),
then, is just run the command:

```sh
./mvnw verify -Dnvd.api.key="<API-KEY>" -DskipTests=true
```

The report will be a `.html` generated on `target/` root.

I deliberately leave this plugin deactivated to permit the application compilation.

## Running

```sh
cp .env.example .env   # fill in DB_URL, DB_PASSWORD, JWT_SECRET, ...
```

From the repo root:

```sh
docker compose up                    # Postgres + Mailpit only, run the API from your IDE/Maven
docker compose --profile app up      # Postgres + Mailpit + this API (port 8080) + the frontend
```

Or run the API alone against an already-running Postgres/Mailpit:

```sh
./mvnw spring-boot:run
```

First boot: set `BOOTSTRAP_MANAGER_*` in `.env` to create the first MANAGER user. Its password is
generated and logged once at startup — rotate it via `POST /auth/password`, then unset those vars.

## Tests

```sh
./mvnw verify
```

Unit tests run with `test`; integration tests (Testcontainers-backed) run in the `verify` phase via
Failsafe. See `docker compose --profile sonar up --abort-on-container-exit sonar-analysis` (repo
root) for the full test + coverage + SonarQube pipeline.

See `HELP.md` for Maven/Spring Boot basics and framework reference links.
