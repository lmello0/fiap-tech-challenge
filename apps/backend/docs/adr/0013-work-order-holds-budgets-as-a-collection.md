# Work Order holds Budgets as a collection, with cardinality enforced only in the application

Exactly one Budget is live per Work Order and there is no requoting (ADR 0008, ADR 0009), so `@OneToOne`
would be the literal mapping. We mapped it as `@OneToMany` with a `getCurrentBudget()` derivation
instead, and put no `UNIQUE` constraint on `budgets.work_order_id`.

Two reasons. With `@OneToOne(mappedBy)` and no database constraint, a bug that creates a second Budget
does not fail on insert — it fails later, on load, with `NonUniqueResultException`, on a work order that
worked yesterday. And renegotiation is a plausible future: a collection is what History wants if it ever
lands, because superseded budgets are precisely what belongs on a Work Order's timeline. Doing it now
costs about what `@OneToOne` would have cost.

The relation itself is enforced, which it previously was not: `budgets.work_order_id` becomes the single
foreign key, `orders.budget_id` is dropped, and `Budget` owns the join. Only the cardinality is left to
the application, so adding requoting later is an application change rather than a migration.
