import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiError } from '../api/api-client';
import { ShopApi, type RegisterCustomerCommand } from '../api/shop-api';
import type { UserInfoDto } from '../api/dto';
import type { WorkerRole } from '../domain/enums';
import type { Worker } from '../domain/models';
import { toWorker } from '../data/mappers';
import { ShopStore } from '../data/shop-store';
import { Directory } from '../data/enrich';
import { DEMO_WORKERS } from '../data/demo-data';
import { TokenStore } from './token-store';

/** What `Session.changePassword` actually left the caller with. */
export type ChangePasswordResult = 'changed' | 'failed' | 'session-lost';

/**
 * Which half of the account is currently being acted on.
 *
 * The manual ships in two volumes. `worker` is the Shop Manual — the staff
 * console. `customer` is the Owner's Manual — your vehicles, your jobs, your
 * bookings. They are separate volumes because the API treats them as separate
 * principals: a CUSTOMER token is refused by every staff endpoint and a staff
 * token is refused by every `/customer-view` one.
 */
export type Facet = 'worker' | 'customer';

/**
 * Where a completed sign-in leaves the caller. Returned rather than acted on,
 * because the screen that asked is the screen that knows where to send them.
 */
export type SignInOutcome =
  /** Exactly one usable facet; it is already active and loaded. */
  | { readonly kind: 'facet'; readonly facet: Facet }
  /** Both facets are usable and none was remembered — the picker decides. */
  | { readonly kind: 'choose' }
  /** Authenticated, but every facet is terminated or deactivated. */
  | { readonly kind: 'none' }
  /** The credentials were right; a `pwd_change` claim confines them to one screen. */
  | { readonly kind: 'password-change' }
  /**
   * The credentials were right, but the account's email has never been
   * confirmed and the API refuses every login until it is. Its own outcome
   * because the recovery is specific — resend the link — and a generic
   * "could not sign in" would send someone to reset a password that is fine.
   */
  | { readonly kind: 'email-unverified'; readonly email: string }
  /** Refused. `error()` carries what the API said. */
  | { readonly kind: 'failed' };

/**
 * Who is at the terminal, and which facet they are acting under.
 *
 * A `User` may carry both a Customer and a Worker facet at once — the shop's
 * own bootstrap account does — and neither may be inferred from the other. So
 * this service holds the *user* as the durable fact and the *facet* as a
 * choice on top of it. Signing in resolves the facets; the picker (or the
 * absence of a choice to make) resolves which one is active; the masthead of
 * either volume switches between them without a round trip to the API.
 *
 * The facet is remembered in `sessionStorage` for the same reason the tokens
 * are: a reload that dumps someone back to the picker they answered two
 * minutes ago is a bug. It is re-validated against the account on every
 * restore, never trusted blind.
 *
 * Demo mode is the third way in. It signs nobody in and calls nothing; it
 * swaps the store's source for the synthetic roster so the staff console can
 * be walked through without a populated database. It is announced in the
 * masthead, never inferred, and never entered because a real sign-in failed.
 */
@Injectable({ providedIn: 'root' })
export class Session {
  private readonly api = inject(ShopApi);
  private readonly tokens = inject(TokenStore);
  private readonly store = inject(ShopStore);
  private readonly directory = inject(Directory);

  private readonly _worker = signal<Worker | null>(null);
  private readonly _user = signal<UserInfoDto | null>(null);
  private readonly _facet = signal<Facet | null>(null);
  private readonly _demo = signal(false);
  private readonly _busy = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _passwordChangeRequired = signal(false);

  readonly worker = this._worker.asReadonly();
  readonly user = this._user.asReadonly();
  readonly facet = this._facet.asReadonly();
  readonly isDemo = this._demo.asReadonly();
  readonly busy = this._busy.asReadonly();
  readonly error = this._error.asReadonly();

  /**
   * Set when the backend's `pwd_change` JWT claim has 403'd a request with
   * "Password change required" — a worker onboarded with a temporary password,
   * or one a manager rotated. Tokens stay held: the caller is authenticated,
   * just confined to the one thing that claim still lets through.
   */
  readonly passwordChangeRequired = this._passwordChangeRequired.asReadonly();

  /* --- facets ------------------------------------------------------------ */

  /** A terminated worker still *has* the facet on the record; they may not use it. */
  readonly hasWorkerFacet = computed(() => {
    const user = this._user();
    return user !== null && user.worker && user.terminationDate === null;
  });

  /** A deactivated customer is the same case: on the record, not usable. */
  readonly hasCustomerFacet = computed(() => {
    const user = this._user();
    return user !== null && user.customer && user.customerActive;
  });

  readonly facets = computed<readonly Facet[]>(() => {
    const both: Facet[] = [];
    if (this.hasWorkerFacet()) both.push('worker');
    if (this.hasCustomerFacet()) both.push('customer');
    return both;
  });

  /** The only case that needs a screen of its own. */
  readonly hasBothFacets = computed(() => this.facets().length === 2);

  /** Signed in to *something* — either volume, or demo mode. */
  readonly signedIn = computed(() => this._user() !== null || this._demo());

  /** Acting on the staff console. Demo mode always is. */
  readonly inWorkshop = computed(() => this._demo() || this._facet() === 'worker');

  /** Acting on the customer console. */
  readonly inGarage = computed(() => !this._demo() && this._facet() === 'customer');

  readonly role = computed<WorkerRole | null>(() => this._worker()?.role ?? null);

  readonly displayName = computed(() => {
    if (this._demo()) return this._worker()?.name ?? '';
    const user = this._user();
    if (!user) return '';
    return [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
  });

  /** The customer facet's own id — the same id as the user's, by construction. */
  readonly customerId = computed(() => this._user()?.id ?? null);

  /** Mirrors `hasAnyRole(...)` on the backend controllers. */
  hasAnyRole(...roles: WorkerRole[]): boolean {
    const r = this.role();
    return r !== null && roles.includes(r);
  }

  /* --- entering ---------------------------------------------------------- */

  /**
   * Restore whatever this tab was doing, if anything.
   *
   * Called once at startup. Demo mode is restored too, and for the same reason
   * a token is: a reload that dumps the operator back to the cover is a bug
   * either way, and demo mode has no token to lean on — just a flag saying the
   * tab had chosen it.
   */
  async restore(): Promise<void> {
    if (readFlag(DEMO_KEY)) {
      await this.enterDemo();
      return;
    }
    if (!this.tokens.hasToken()) return;
    this._busy.set(true);
    try {
      this._user.set(await this.api.me());
      await this.resume();
    } catch (error) {
      if (isPasswordChangeRequired(error)) {
        this._passwordChangeRequired.set(true);
      } else {
        this.tokens.clear();
        this._user.set(null);
      }
    } finally {
      this._busy.set(false);
    }
  }

  async signIn(email: string, password: string): Promise<SignInOutcome> {
    this._busy.set(true);
    this._error.set(null);
    this._passwordChangeRequired.set(false);
    try {
      const token = await this.api.login(email, password);
      this.tokens.set(token.accessToken, token.refreshToken);
      this._user.set(await this.api.me());
      return await this.resume();
    } catch (error) {
      if (isPasswordChangeRequired(error)) {
        // The credentials were right — tokens stay held, just confined to
        // /auth/password/change until the operator rotates it.
        this._passwordChangeRequired.set(true);
        return { kind: 'password-change' };
      }
      this.tokens.clear();
      this._user.set(null);
      if (isEmailUnverified(error)) return { kind: 'email-unverified', email };
      this._error.set(error instanceof ApiError ? error.message : 'Could not sign in.');
      return { kind: 'failed' };
    } finally {
      this._busy.set(false);
    }
  }

  /**
   * Create a Customer facet and sign in with it, in one step.
   *
   * `POST /auth/register/customer` returns the token pair itself, so there is
   * no second `login` to make and no window in which the account exists but
   * the person is not signed into it. Registration only ever produces a
   * Customer, so the outcome is never the picker.
   */
  async register(command: RegisterCustomerCommand): Promise<SignInOutcome> {
    this._busy.set(true);
    this._error.set(null);
    try {
      const token = await this.api.registerCustomer(command);
      this.tokens.set(token.accessToken, token.refreshToken);
      this._user.set(await this.api.me());
      return await this.resume();
    } catch (error) {
      this.tokens.clear();
      this._user.set(null);
      this._error.set(
        error instanceof ApiError ? error.message : 'Could not create the account.',
      );
      return { kind: 'failed' };
    } finally {
      this._busy.set(false);
    }
  }

  /**
   * Decide which volume an adopted user opens on.
   *
   * A remembered facet wins, but only after it is re-checked against the
   * account: a worker terminated since the last visit, or a customer facet
   * deactivated, must not be restored into a console every request would
   * refuse. One usable facet skips the picker entirely — a screen offering a
   * single choice is a screen asking the person to confirm a fact.
   */
  private async resume(): Promise<SignInOutcome> {
    const available = this.facets();
    if (available.length === 0) {
      const user = this._user();
      this.tokens.clear();
      this._user.set(null);
      this._error.set(
        user?.worker
          ? 'That worker account has been terminated, and it holds no active customer account.'
          : 'That account has been deactivated.',
      );
      return { kind: 'none' };
    }

    const remembered = readFacet();
    const chosen = remembered && available.includes(remembered) ? remembered : null;

    if (chosen) {
      await this.activate(chosen);
      return { kind: 'facet', facet: chosen };
    }
    if (available.length === 1) {
      await this.activate(available[0]);
      return { kind: 'facet', facet: available[0] };
    }
    writeFacet(null);
    this._facet.set(null);
    return { kind: 'choose' };
  }

  /**
   * Take up one facet.
   *
   * Only the worker facet primes `ShopStore`: it loads the whole shop, and
   * every one of those reads is a staff endpoint a CUSTOMER principal would be
   * refused. The customer volume loads its own, much smaller, world from its
   * own shell.
   */
  async activate(facet: Facet): Promise<void> {
    const user = this._user();
    if (!user) return;
    if (!this.facets().includes(facet)) return;

    this._facet.set(facet);
    writeFacet(facet);
    this._error.set(null);

    if (facet === 'worker') {
      this._worker.set(toWorker(user));
      this.store.actingWorkerId = user.id;
      this.store.setMode('live');
      await this.store.load(true);
    } else {
      // Nothing staff-shaped survives the switch: leaving a loaded shop behind
      // a customer masthead is how a stale read ends up on the wrong screen.
      this._worker.set(null);
      this.store.reset();
    }
  }

  /** Re-read the account after a profile edit, keeping the active facet. */
  async refreshUser(): Promise<void> {
    if (this._demo()) return;
    try {
      this._user.set(await this.api.me());
    } catch {
      // A failed refresh leaves the last known good user in place; the screen
      // that edited already has the authoritative answer from its own response.
    }
  }

  /**
   * The single screen behind both a forced password rotation and a voluntary
   * one from a signed-in account's own settings.
   *
   * `POST /auth/password/change` revokes every refresh token for the account
   * — this tab's included — and returns no new token pair. A forced change's
   * access token would also keep carrying its stale `pwd_change` claim, since
   * a JWT's claims are fixed at mint time. Either way the tokens this tab is
   * holding are dead the instant the call succeeds, so the only correct next
   * step is a fresh `login` with the password that was just set, not a reuse
   * of anything already in hand.
   */
  async changePassword(
    email: string,
    currentPassword: string,
    newPassword: string,
  ): Promise<ChangePasswordResult> {
    this._busy.set(true);
    this._error.set(null);
    try {
      await this.api.changePassword(currentPassword, newPassword);
    } catch (error) {
      this._error.set(
        error instanceof ApiError ? error.message : 'Could not change the password.',
      );
      this._busy.set(false);
      return 'failed';
    }

    try {
      const token = await this.api.login(email, newPassword);
      this.tokens.set(token.accessToken, token.refreshToken);
      this._passwordChangeRequired.set(false);
      this._user.set(await this.api.me());
      const outcome = await this.resume();
      return outcome.kind === 'failed' || outcome.kind === 'none' ? 'session-lost' : 'changed';
    } catch {
      // The password did change — only the automatic re-sign-in failed. There
      // is nothing left to retry with: the old tokens are as dead as if this
      // had succeeded, so drop them and send the operator back to sign in by
      // hand rather than pretend a session is still open.
      this.tokens.clear();
      this._worker.set(null);
      this._user.set(null);
      this._facet.set(null);
      writeFacet(null);
      this._passwordChangeRequired.set(false);
      this._error.set('Your password was changed. Sign in again with your new password.');
      return 'session-lost';
    } finally {
      this._busy.set(false);
    }
  }

  /** Walk the staff console against the synthetic roster. Signs in to nothing. */
  async enterDemo(worker: Worker = DEMO_WORKERS[2]): Promise<void> {
    this._demo.set(true);
    writeFlag(DEMO_KEY, true);
    this._error.set(null);
    this._user.set(null);
    this._facet.set('worker');
    this._worker.set(worker);
    this.store.actingWorkerId = worker.id;
    this.store.setMode('demo');
    await this.store.load(true);
  }

  /** Demo only: walk the console as each role. Absent when signed in for real. */
  async switchDemoWorker(worker: Worker | null): Promise<void> {
    if (!this._demo() || !worker) return;
    this._worker.set(worker);
    this.store.actingWorkerId = worker.id;
  }

  async signOut(): Promise<void> {
    const refresh = this.tokens.refreshToken();
    if (refresh) {
      // Best effort: the local session ends either way.
      await this.api.logout(refresh).catch(() => undefined);
    }
    this.tokens.clear();
    this.directory.clear();
    this.store.reset();
    writeFlag(DEMO_KEY, false);
    writeFacet(null);
    this._worker.set(null);
    this._user.set(null);
    this._facet.set(null);
    this._demo.set(false);
    this._error.set(null);
    this._passwordChangeRequired.set(false);
  }

  clearError(): void {
    this._error.set(null);
  }
}

/** Mirrors `AuthServiceImpl`: every login is refused until the address is confirmed. */
function isEmailUnverified(error: unknown): boolean {
  return (
    error instanceof ApiError && error.status === 403 && error.problem?.title === 'Email not verified'
  );
}

/** Mirrors the backend's `PasswordChangeRequiredFilter`, which writes this exact title. */
function isPasswordChangeRequired(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    error.status === 403 &&
    error.problem?.title === 'Password change required'
  );
}

const DEMO_KEY = 'ars.demo';
const FACET_KEY = 'ars.facet';

/** Wrapped like the token store's accessors: a terminal may refuse site data. */
function readFlag(key: string): boolean {
  try {
    return sessionStorage.getItem(key) === '1';
  } catch {
    return false;
  }
}

function writeFlag(key: string, on: boolean): void {
  try {
    if (on) sessionStorage.setItem(key, '1');
    else sessionStorage.removeItem(key);
  } catch {
    // Still works for this page view; it just will not survive a reload.
  }
}

function readFacet(): Facet | null {
  try {
    const value = sessionStorage.getItem(FACET_KEY);
    return value === 'worker' || value === 'customer' ? value : null;
  } catch {
    return null;
  }
}

function writeFacet(facet: Facet | null): void {
  try {
    if (facet) sessionStorage.setItem(FACET_KEY, facet);
    else sessionStorage.removeItem(FACET_KEY);
  } catch {
    // A picked facet that does not survive a reload just shows the picker again.
  }
}
