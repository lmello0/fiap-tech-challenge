# Users & Auth

Identity and account management for the platform: who a person is (`User` and its facets), how they prove it (`auth`), and how access is granted or revoked.

## Language

**User**:
A person registered in the system, identified by email and document. A `User` is a shell that can carry one or both of two facets — `Customer` and `Worker` — at the same time.
_Avoid_: Person, Account (as a synonym for User)

**Customer facet**:
The facet of a `User` that lets them request work orders. Carries its own active/deactivated state, independent of any `Worker` facet the same `User` might also have.
_Avoid_: Client, Buyer

**Worker facet**:
The facet of a `User` that lets them perform work-order tasks (mechanic, attendant, manager). Carries a `role`, hire/start dates, and its own active/terminated state.
_Avoid_: Employee, Staff

**Deactivation**:
Soft-delete of the `Customer` facet, triggered by the customer themselves, an Attendant, or a Manager. Reversible via **Reactivation** (Attendant/Manager only). Does not touch the `Worker` facet of the same `User`, if any.
_Avoid_: Delete, Ban

**Reactivation**:
Restoring a deactivated `Customer` facet to active. Staff-only action.

**Termination**:
Ending a `Worker` facet's employment as of a given date. Distinct from `Deactivation` — termination is an HR-flavored, one-directional event (no reactivation flow), while deactivation is a reversible self-service or staff action on the `Customer` facet.
_Avoid_: Deactivation (when talking about a Worker), Firing

**Account access**:
A `User` may log in as long as at least one of their facets (`Customer` or `Worker`) is active. If every facet the `User` has is deactivated/terminated, login is refused.

**Password Reset Token**:
A single-use, time-limited token proving control of a `User`'s registered email, used to set a new password without knowing the current one (the "magic link" flow). Distinct from an authenticated password change, which proves control by supplying the current password instead.
_Avoid_: Magic token, Reset code

**Email Verification Token**:
A single-use, time-limited token proving control of the email a `User` registered with. Until consumed, the account is unverified and login is refused.

**Email Change Token**:
A single-use, time-limited token proving control of a *new* email address a `User` wants to switch to. The registered email only changes once this token is consumed (double opt-in) — the old email keeps working for login until then.

**Refresh Token**:
An opaque, rotating credential used to obtain new access tokens without re-authenticating. Each use retires the token and issues a successor; presenting an already-retired token is treated as token theft and revokes every session for that `User`.
