import { Injectable, computed, signal } from '@angular/core';
import type { WorkerRole } from '../domain/enums';
import type { Worker } from '../domain/models';
import { DEMO_WORKERS } from '../data/demo-data';

/**
 * Who is at the terminal.
 *
 * A `User` may carry both a Customer and a Worker facet at once; this console
 * only ever acts on the Worker facet, and refuses entry when no Worker facet is
 * active. Against the live API this is populated from `GET /users/me` after
 * login; in demo mode it is seeded from the synthetic worker roster so every
 * role can be walked through without a running backend.
 */
@Injectable({ providedIn: 'root' })
export class Session {
  /** Demo mode is on whenever the console has no live API behind it. */
  readonly isDemo = signal(true);

  private readonly _worker = signal<Worker | null>(DEMO_WORKERS[2]); // Manager
  readonly worker = this._worker.asReadonly();

  readonly role = computed<WorkerRole | null>(() => this._worker()?.role ?? null);

  readonly signedIn = computed(() => this._worker() !== null);

  /** Login is refused outright when every facet a user holds is inactive. */
  readonly hasActiveFacet = computed(() => this._worker()?.active ?? false);

  readonly displayName = computed(() => this._worker()?.name ?? '');

  /** Set once by the login flow, or by the demo role switcher. */
  setWorker(worker: Worker | null): void {
    this._worker.set(worker);
  }

  signOut(): void {
    this._worker.set(null);
  }

  /** Mirrors `hasAnyRole(...)` on the backend controllers. */
  hasAnyRole(...roles: WorkerRole[]): boolean {
    const r = this.role();
    return r !== null && roles.includes(r);
  }
}
