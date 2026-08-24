import { Component, computed, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { Session } from '../../core/auth/session';
import { CustomerStore } from '../../core/customer/customer-store';
import { CUSTOMER_SECTIONS } from '../../core/nav';
import { Icon } from '../../shared/ui/icon';

/**
 * The Owner's Manual — volume two.
 *
 * The same book as the staff console: thumb tabs numbered to their own index,
 * a masthead stamped with who is reading, plates and rules underneath. It is
 * not a stripped-down staff console and does not read as one — the sections
 * are the customer's own (their vehicles, their jobs, their bookings), the
 * numbering starts at 1 again because it is a different volume, and nothing
 * here is a permission-filtered view of a screen that exists elsewhere.
 *
 * Unlike the staff console, this one adapts. The console is a fixed desktop
 * workstation by decision; this is read by people standing next to a car, so
 * the tab rail folds to a scrolling strip rather than assuming a desk.
 */
@Component({
  selector: 'app-garage-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon],
  templateUrl: './garage-shell.html',
  styleUrl: './garage-shell.scss',
})
export class GarageShell {
  protected readonly session = inject(Session);
  protected readonly store = inject(CustomerStore);
  private readonly router = inject(Router);

  protected readonly sections = CUSTOMER_SECTIONS;
  protected readonly revision = '2026.08';

  constructor() {
    void this.store.load();
  }

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  protected readonly section = computed(() => {
    const path = this.url().split('?')[0];
    return this.sections.find((s) => path.startsWith(`/${s.path}`)) ?? null;
  });

  /** The two counts worth carrying into the masthead, because both are a queue on the reader. */
  protected readonly decisions = computed(() => this.store.awaitingDecision().length);
  protected readonly pickups = computed(() => this.store.readyForPickup().length);

  protected async switchToWorkshop(): Promise<void> {
    await this.session.activate('worker');
    await this.router.navigateByUrl('/work-orders');
  }

  protected async signOut(): Promise<void> {
    await this.session.signOut();
    this.store.reset();
    await this.router.navigateByUrl('/');
  }

  protected reload(): void {
    void this.store.load(true);
  }
}
