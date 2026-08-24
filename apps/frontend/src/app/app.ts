import { Component, computed, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { Session } from './core/auth/session';
import { ShopStore } from './core/data/shop-store';
import { SECTIONS, sectionsFor } from './core/nav';
import { WORKER_ROLE_LABEL, type WorkerRole } from './core/domain/enums';
import { Icon } from './shared/ui/icon';


@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly session = inject(Session);
  protected readonly store = inject(ShopStore);
  private readonly router = inject(Router);

  /** Printed on the masthead and the tab foot, the way a manual is dated. */
  protected readonly revision = '2026.08';

  protected readonly workers = this.store.workers;

  protected readonly sections = computed(() => sectionsFor(this.session.role()));

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  protected readonly section = computed(() => {
    const path = this.url().split('?')[0].split('/')[1] ?? '';
    return SECTIONS.find((s) => s.path === path) ?? null;
  });

  protected roleLabel(role: WorkerRole): string {
    return WORKER_ROLE_LABEL[role];
  }

  /**
   * Demo affordance only: walk the console as each role without a populated
   * database. Against the live API the signed-in worker comes from
   * `GET /users/me` and this control is not rendered at all.
   */
  protected async switchWorker(event: Event): Promise<void> {
    const id = (event.target as HTMLSelectElement).value;
    const worker = this.store.workers().find((w) => w.id === id) ?? null;
    await this.session.switchDemoWorker(worker);

    // A role change can revoke the open section; land somewhere lawful.
    const allowed = sectionsFor(worker?.role ?? null);
    const open = this.section();
    if (!open || !allowed.some((s) => s.path === open.path)) {
      await this.router.navigate([allowed[0]?.path ?? '/']);
    }
  }

  /** Leave for the owner's manual. The staff cache is dropped by the session. */
  protected async switchToGarage(): Promise<void> {
    await this.session.activate('customer');
    await this.router.navigateByUrl('/my');
  }

  protected async signOut(): Promise<void> {
    await this.session.signOut();
    await this.router.navigateByUrl('/');
  }

  /** Re-run the whole load. The banner's only offer, and it is the right one. */
  protected reload(): void {
    void this.store.load(true);
  }
}
