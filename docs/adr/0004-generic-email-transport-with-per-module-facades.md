# Generic email transport with per-module facades

Email delivery lives in `shared.email`, and the only thing it knows how to do is send an
`EmailRequestedEvent`: recipients, a subject, a plain text body, an HTML body, or both. It knows
nothing about password resets, verification links, or work orders.

The obvious alternative was to let the email module listen to the domain events that should produce
mail — `PasswordResetRequested`, `WorkOrderApproved` — and own the templates for each. That inverts
the module graph the wrong way: `shared` would depend on `auth`, while `auth` already depends on
`shared`, and `ModularityTests` would fail on the cycle. Keeping the contract in `shared.email.api`
and having producers publish it points every dependency the same way it already points, and it means
a new module needs no change to the email module at all.

The cost is that each producing module owns the wording of its own mail. `auth` gets `AuthEmails`, a
plain class its services call, which turns a token flow into a subject and a body and publishes.
Every module that sends mail is expected to have one; services never publish `EmailRequestedEvent`
themselves, so there is exactly one place per module that knows what its emails say. We accepted the
duplication of composition over `shared` accumulating every module's copy, which is the same trade
ADR 0002 and ADR 0003 made for token plumbing.

`EmailDispatcher` is package-private and outside the named interface, so `modules.verify()` fails if
any module reaches for it. That guarantee has a hole we knowingly left open: `spring-boot-starter-mail`
publishes a `JavaMailSender` bean into the context, and nothing stops a service from injecting it and
sending mail directly. Removing the bean (constructing the sender privately inside `shared.email`)
would have closed it, at the cost of hand-binding mail configuration instead of using `spring.mail.*`.
The rule is documented here rather than enforced.

Delivery is asynchronous and durable. `@ApplicationModuleListener` dispatches after the publishing
transaction commits, and Spring Modulith persists each publication in `event_publication`, so a
process that dies mid-send leaves a record rather than losing the mail. What Modulith does *not* do is
resubmit anything on its own — the staleness monitor only marks stale publications FAILED — so
`RetryFailedEmails` is the trigger, running every minute over FAILED publications, oldest first,
filtered to `EmailRequestedEvent` and capped at `app.mail.max-attempts` passes. `spring.modulith.events.staleness.resubmitted`
is load-bearing: without it a retry that fails again stays RESUBMITTED, never returns to FAILED, and
the retry budget silently becomes one.

Because retries can land minutes after the fact, each message carries its own `expiresAt`, and
`AuthEmails` derives it from the very TTL that stamped the token it links to. A reset email whose
token has already died is dropped rather than delivered, which is why the dispatcher throws
`EmailExpiredException` instead of returning quietly: returning would let Modulith mark the
publication COMPLETED and record an email that was never sent as if it had been. Left incomplete, it
becomes FAILED and stays queryable as "never delivered" until `PurgeEventPublications` collects it
after 30 days. `republish-outstanding-events-on-restart` is off for the same reason — it would re-fire
everything at boot, ignoring both the filter and the expiry.
