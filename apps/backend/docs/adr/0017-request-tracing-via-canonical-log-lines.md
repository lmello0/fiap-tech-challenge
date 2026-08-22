# Request tracing via `requestId` and canonical log lines, not per-statement logging

Every unit of work — an HTTP request, a Spring Modulith `@ApplicationModuleListener` invocation, a
`@Scheduled`/ShedLock job run — gets one accumulated, structured log line emitted when it finishes,
instead of many separate `log.info(...)` calls scattered through the call stack. Fields (actor,
entities touched, outcome, timing, error detail) get added to a context object as execution passes
through it, and are flushed as a single JSON line at the end. The obvious alternative — leaving each
existing call site alone and just making its individual line "wide" (structured key=value instead of
free text) — was rejected: with 36 files already doing ad hoc `@Slf4j` logging, that keeps the real
problem, that tracing one request means correlating N scattered lines by eye, rather than solving it.

`requestId` identifies the flow: read from an incoming `X-Request-Id` header if the client sent one,
otherwise generated as a UUID, and always echoed back in the response header and in `ProblemDetail`
error bodies. It's a distinct concept from the existing `correlationId` field used internally by
`email`/`workorder.BudgetServiceImpl` to tie an email send attempt to its result — that name was
already taken by a narrower domain concept, so tracing gets its own name rather than overloading it.

The harder problem is that this app has no synchronous downstream calls to correlate — everything
that crosses a thread boundary does so through Spring Modulith's `@ApplicationModuleListener`
methods (7 of them, across 6 classes), which dispatch asynchronously *after commit*, backed by
events Modulith persists to `event_publication` for replay (see ADR 0004). A `requestId` carried
only in `MDC`/`ThreadLocal` would be correct for the common case and silently wrong for any listener
invocation that runs on retry or after a restart, since the originating thread and its context are
long gone by then. So the id has to travel with the event itself, and most events already have
somewhere to put it: `EventMetadata` — the envelope every `DomainEvent` carries for `history`'s sake
(ADR 0011) — got a `requestId` field alongside its existing `actorType`/`actorLabel`, populated
automatically by `ActorResolver` (the one place that builds it) rather than by every publisher. That
covers 47 of the app's 52 event types for free. The remaining 5 don't implement `DomainEvent`; of
those, the 3 `email` transport events (`EmailRequestedEvent` and its two delivery-outcome events)
are the ones that actually cross an async boundary, so each grew its own `requestId` component,
self-populated in a compact constructor the same way, rather than inventing a shared interface only
three records would implement. Either way, listener code reads the id the same way it already reads
everything else about the event that triggered it — no new abstraction, no per-listener boilerplate
beyond opening a `LogContext` scope with whatever the event reports.

`@Scheduled` jobs have no originating request to inherit an id from, so each run generates its own
fresh `requestId` at start and gets its own canonical line, same as a request or a listener
invocation — three contexts, one logging shape, so "trace the whole flow" doesn't mean learning three
different conventions depending on where a bug happens to surface.

`History` (the customer-facing, append-only audit trail — ADR 0011) deliberately does not carry
`requestId`. History and request tracing answer different questions for different audiences — "what
happened to this Work Order" versus "what did the server do to produce that" — and collapsing them
risks an ops-only identifier leaking into a `Timeline` a customer's own view might expose.
