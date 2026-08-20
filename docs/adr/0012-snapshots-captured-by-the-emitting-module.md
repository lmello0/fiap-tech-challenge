# Snapshots are captured by the emitting module, inside the publishing transaction

History records not just that something happened but what the aggregate looked like when it did, so the
work order screen can show the state at any past moment without replaying anything. The obvious
implementation — let the history listener load the entity and serialize it — is wrong here.
`@ApplicationModuleListener` runs after commit on another thread, so two events seconds apart would both
read the same latest state and both record it against the earlier moment. The snapshot would be wrong
exactly when the work order is moving fastest.

So each domain event carries its own snapshot, assembled by the module that owns the aggregate, inside
the transaction that produced the change. Events implement `DomainEvent` from `shared.audit`, which
carries event metadata — id, timestamp, actor, and a stable string event code declared by the event
rather than derived from its class name, because these rows outlive the classes that wrote them — plus
an opaque snapshot document. History subscribes to `DomainEvent` and stores the document as `jsonb`
without deserializing it, alongside a `schema_version`, since a snapshot frozen today must stay
readable after the entity it describes has gained and lost fields.

The consequence worth stating: `history` imports nothing from `workorder`, `inventory`, `user`, or
`vehicle`. Adding history for a new module requires no change to `history` at all. The write to history
stays asynchronous; only the capture is synchronous. The price is that every published event grows a
snapshot and every emitting service assembles one — paid deliberately, because the alternative is a
history that is quietly wrong rather than expensively right.
