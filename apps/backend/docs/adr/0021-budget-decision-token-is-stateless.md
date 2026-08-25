# Budget Decision Token is a stateless HMAC-signed capability, not a persisted opaque token

Every other token in this codebase (Password Reset, Email Verification/Change, the three Appointment
guest tokens) is a random opaque value with only its hash persisted, looked up by that hash. The
Budget Decision Token — mailed alongside a sent Budget so a customer can approve or refuse without
signing in — breaks that pattern on purpose: it is `budgetId + "." + HMAC-SHA256(secret, budgetId)`,
verified by recomputing the signature, and nothing is written to the database for it.

This follows directly from three product decisions: the token must never expire on a clock (it dies
only when the Budget resolves — checked via the existing `BudgetStateMachine` transition rules, the
same ones the authenticated approve/refuse path already runs through), it must be reusable rather than
single-use, and `resend` must re-mail the *same* link rather than invalidate the one already sent. An
opaque-token-plus-hash design can satisfy the first two but not the third: once only a hash is stored,
the raw value handed out in the first email can never be recovered to put in a second one. The only
way to honor "resend re-mails the same link" without storing the raw token in plaintext (which every
other token in this codebase deliberately avoids) is for the token to be recomputable from data already
in hand — i.e., stateless.

The trade-off: rotating `BUDGET_DECISION_TOKEN_SECRET` invalidates every outstanding link at once
(acceptable — the authenticated approve/refuse path still works, and a customer with a dead link can
be resent one), and this is now the one place in the codebase where "does this token exist" is not a
database question.
