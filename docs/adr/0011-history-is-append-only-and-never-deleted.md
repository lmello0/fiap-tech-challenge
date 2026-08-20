# History is append-only and never deleted

Every other table in this system has a purge path — `PurgeExpiredRefreshTokens`,
`PurgeEventPublications`, `ExpireStaleReservations`. History deliberately has none, and the database
enforces it: the `history` schema carries a `BEFORE UPDATE OR DELETE` trigger that raises, so an entry
cannot be rewritten or removed by the application, by a scheduled job, or by anyone with a psql
session. There is no retention window and no purge cron.

The alternative — splitting the single `DB_USER` into a migration role and a runtime role granted only
`INSERT`/`SELECT`, plus a `SECURITY DEFINER` function driving a multi-year retention sweep — was
rejected as cost without a matching requirement. Grants alone do not stop someone connecting directly;
the trigger does, for one migration and no operational change. And "history is never deleted" is a
simpler rule to defend than any retention policy.

Two consequences worth stating. Flyway needs a documented way past its own trigger for future
migrations that touch the `history` schema. And History deliberately duplicates state other modules
already record — `WorkOrder`'s nine lifecycle timestamps, `Stock Movement`, `Service Execution`. Those
stay exactly where they are, because business logic reads them: `ServiceExecution` feeds the rolling
execution-time average, `deliveredAt` gates transitions. Folding them into History would make
`workorder` and `inventory` depend on `history` and invert the one-way flow the module exists to
preserve.

One durability gap this ADR knowingly leaves open. `spring.modulith.events.republish-outstanding-events-on-restart`
stays `false`: it is a single registry-wide setting, not one `history` can opt into on its own, and
flipping it would also replay every incomplete `email` publication on restart — bypassing
`RetryFailedEmails`, which owns that recovery path deliberately (see `application.yaml`) and can
resend a mail that already went out. Email correctness wins; the cost is that a crash between a
publishing transaction's commit and `HistoryEntryWriter` running loses that one history row for good,
with nothing to replay it from. Given `history` was scoped as best-effort and approximate (not a
compliance-grade audit trail), that gap was accepted rather than paid for with a regression to a
module that already shipped.
