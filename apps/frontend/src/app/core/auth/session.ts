import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiError } from '../api/api-client';
import { ShopApi } from '../api/shop-api';
import type { UserInfoDto } from '../api/dto';
import type { WorkerRole } from '../domain/enums';
import type { Worker } from '../domain/models';
import { toWorker } from '../data/mappers';
import { ShopStore } from '../data/shop-store';
import { Directory } from '../data/enrich';
import { DEMO_WORKERS } from '../data/demo-data';
import { TokenStore } from './token-store';

/**
 * Who is at the terminal.
 *
 * A `User` may carry both a Customer and a Worker facet at once — the shop's
 * own bootstrap account does — and this console only ever acts on the Worker
 * facet. A user with no active Worker facet is refused entry outright rather
 * than shown a console they cannot use, which is also exactly what the API
 * would do to every request they made.
 *
 * Demo mode is the second way in. It signs nobody in and calls nothing; it
 * swaps the store's source for the synthetic roster so the console can be
 * walked through without a populated database. It is announced in the masthead,
 * never inferred, and never entered because a real sign-in failed.
 */
@Injectable({ providedIn: 'root' })
export class Session {
  private readonly api = inject(ShopApi);
  private readonly tokens = inject(TokenStore);
  private readonly store = inject(ShopStore);
  private readonly directory = inject(Directory);

  private readonly _worker = signal<Worker | null>(null);
  private readonly _user = signal<UserInfoDto | null>(null);
  private readonly _demo = signal(false);
  private readonly _busy = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly worker = this._worker.asReadonly();
  readonly user = this._user.asReadonly();
  readonly isDemo = this._demo.asReadonly();
  readonly busy = this._busy.asReadonly();
  readonly error = this._error.asReadonly();

  readonly role = computed<WorkerRole | null>(() => this._worker()?.role ?? null);
  readonly signedIn = computed(() => this._worker() !== null);
  readonly displayName = computed(() => this._worker()?.name ?? '');

  /** True when the user also holds a Customer facet — worth saying, never acted on. */
  readonly alsoCustomer = computed(() => this._user()?.customer === true);

  /** Mirrors `hasAnyRole(...)` on the backend controllers. */
  hasAnyRole(...roles: WorkerRole[]): boolean {
    const r = this.role();
    return r !== null && roles.includes(r);
  }

  /**
   * Restore whatever this tab was doing, if anything.
   *
   * Called once at startup. Demo mode is restored too, and for the same reason
   * a token is: a reload that dumps the operator back to the cover is a bug
   * either way, and demo mode has no token to lean on — just a flag saying the
   * tab had chosen it.
   */
  async restore(): Promise<void> {
    if (readDemoFlag()) {
      await this.enterDemo();
      return;
    }
    if (!this.tokens.hasToken()) return;
    this._busy.set(true);
    try {
      await this.adopt(await this.api.me());
    } catch {
      this.tokens.clear();
    } finally {
      this._busy.set(false);
    }
  }

  async signIn(email: string, password: string): Promise<boolean> {
    this._busy.set(true);
    this._error.set(null);
    try {
      const token = await this.api.login(email, password);
      this.tokens.set(token.accessToken, token.refreshToken);
      await this.adopt(await this.api.me());
      return this.signedIn();
    } catch (error) {
      this.tokens.clear();
      this._error.set(
        error instanceof ApiError ? error.message : 'Could not sign in.',
      );
      return false;
    } finally {
      this._busy.set(false);
    }
  }

  /**
   * Take up the Worker facet of a signed-in user, or refuse.
   *
   * The refusal is deliberate and specific: an account with only a Customer
   * facet is not a lesser member of staff, it is not staff, and every screen
   * behind this point would 403 on its first request.
   */
  private async adopt(user: UserInfoDto): Promise<void> {
    if (!user.worker || user.terminationDate !== null) {
      this.tokens.clear();
      this._error.set(
        user.worker
          ? 'That worker account has been terminated.'
          : 'That account has no staff access to this console.',
      );
      return;
    }

    this._user.set(user);
    this._worker.set(toWorker(user));
    this.store.actingWorkerId = user.id;
    this.store.setMode('live');
    await this.store.load(true);
  }

  /** Walk the console against the synthetic roster. Signs in to nothing. */
  async enterDemo(worker: Worker = DEMO_WORKERS[2]): Promise<void> {
    this._demo.set(true);
    writeDemoFlag(true);
    this._error.set(null);
    this._user.set(null);
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
    writeDemoFlag(false);
    this._worker.set(null);
    this._user.set(null);
    this._demo.set(false);
    this._error.set(null);
  }

  clearError(): void {
    this._error.set(null);
  }
}

const DEMO_KEY = 'ars.demo';

/** Wrapped like the token store's accessors: a terminal may refuse site data. */
function readDemoFlag(): boolean {
  try {
    return sessionStorage.getItem(DEMO_KEY) === '1';
  } catch {
    return false;
  }
}

function writeDemoFlag(on: boolean): void {
  try {
    if (on) sessionStorage.setItem(DEMO_KEY, '1');
    else sessionStorage.removeItem(DEMO_KEY);
  } catch {
    // Demo mode still works for this page view; it just will not survive a reload.
  }
}
