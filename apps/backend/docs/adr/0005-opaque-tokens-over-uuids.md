# Opaque high-entropy tokens over UUIDs

Every token flow in auth — refresh, password reset, email verification, email change — hands the
holder a secret that grants an action, so the obvious question is why they aren't just `UUID`s, given
the type is already everywhere in the codebase. `UUID.randomUUID()` is not the predictability trap it
is sometimes assumed to be; it draws from `SecureRandom`. The problem is what surrounds it. A v4 UUID
spends six of its 128 bits on the version and variant nibbles, leaving 122 bits of randomness — just
under the ≥128 bits expected of a session-grade secret, for no gain. And `UUID` already carries a
meaning here: it is what an *identifier* looks like. `userId`, `RefreshToken.id`, and `replacedById`
are logged, returned in responses, and compared with `equals`; token values are hashed with SHA-256
before they touch a row, precisely so a database dump cannot be replayed. Giving both the same type
erases the only cue distinguishing the values that may be printed from the ones that never may — and
these live side by side, a `log.info(... userId={} ...)` one line from the raw token. The last risk is
forward-looking: UUIDv7 is now the fashionable choice for database keys, and it is time-ordered, so a
future sweep to "standardize on v7" would quietly make these tokens partially derivable from the
moment their email was sent.

So each flow generates 32 bytes (256 bits) through Spring's `Base64StringKeyGenerator`, keeping its
own copy of the generator and `hash()` for the reasons ADR 0003 gives. The generator is configured
with `Base64.getUrlEncoder().withoutPadding()` rather than the default: standard Base64 emits `+`,
`/` and `=`, which forced `AuthEmails.link()` to percent-encode every link and left the "paste this
code into the app" fallback beneath it in a different encoding than the link above it. URL-safe
Base64 makes the raw token a legal query parameter as-is, so the link and the copyable code are the
same string. Changing the alphabet needs no migration — rows store a hash of whatever string was
issued, and validation hashes whatever the client presents, so tokens already in flight keep working
until they expire.
