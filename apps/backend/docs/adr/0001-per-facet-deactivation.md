# Deactivation state lives on the facet, not on User

A `User` can carry a `Customer` facet, a `Worker` facet, or both at once. When we needed a "delete" for the customer-facing CRUD, the obvious shortcut was a single `active`/`deletedAt` flag on `User` — but that would deactivate both facets together, even though a Customer deactivating their own account has nothing to do with a Worker's employment status on the same account.

We instead give each facet its own active/inactive state: `Customer` gets a self-service, reversible `Deactivation`/`Reactivation`; `Worker` keeps its existing one-directional `Termination`. A `User` can still log in as long as *any* facet is active. This costs an extra flag per facet and a login rule that checks across facets instead of one field, but keeps the two lifecycles (a customer closing their account vs. an employee leaving) from bleeding into each other.
