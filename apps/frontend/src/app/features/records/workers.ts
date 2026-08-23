import { Component, computed, inject } from '@angular/core';
import { ShopStore } from '../../core/data/shop-store';
import { WORKER_ROLE_LABEL, type WorkerRole } from '../../core/domain/enums';
import { Callout } from '../../shared/ui/callout';

@Component({
  selector: 'app-workers',
  imports: [Callout],
  template: `
    <div class="rec">
      <header class="rec__head">
        <div>
          <h1 class="rec__title">Workers</h1>
          <p class="rec__sub">{{ active().length }} active · {{ terminated().length }} terminated</p>
        </div>
      </header>

      <div class="rec__notice">
        <app-callout tier="warning">
          Termination is one-directional. Unlike a customer deactivation there is no reactivation
          flow — a terminated worker facet stays terminated, and if it was the person's only active
          facet they can no longer sign in at all.
        </app-callout>
      </div>

      <section class="plate">
        <div class="plate__head">
          <p class="plate__title">9.1 — Roster</p>
          <p class="plate__meta label">Manager only</p>
        </div>
        <div class="mt-wrap">
          <table class="mt">
            <caption class="sr-only">Workers with role and employment dates.</caption>
            <thead>
              <tr>
                <th scope="col">Name</th>
                <th scope="col">Email</th>
                <th scope="col" class="shrink">Role</th>
                <th scope="col" class="shrink">Hired</th>
                <th scope="col" class="shrink">Started</th>
                <th scope="col" class="shrink">Terminated</th>
              </tr>
            </thead>
            <tbody>
              @for (w of rows(); track w.id) {
                <tr [class.is-inactive]="!w.active">
                  <td class="rec__name">{{ w.name }}</td>
                  <td class="rec__muted">{{ w.email }}</td>
                  <td class="shrink">
                    <span class="rec__role" [class]="'is-' + w.role.toLowerCase()">{{
                      roleLabel(w.role)
                    }}</span>
                  </td>
                  <td class="shrink rec__muted">{{ w.hiredAt }}</td>
                  <td class="shrink rec__muted">{{ w.startedAt ?? '—' }}</td>
                  <td class="shrink">
                    @if (w.terminatedAt) {
                      <span class="rec__state is-off">{{ w.terminatedAt }}</span>
                    } @else {
                      <span class="rec__muted">—</span>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `,
  styleUrl: './records.scss',
})
export class Workers {
  private readonly store = inject(ShopStore);

  protected readonly rows = computed(() =>
    [...this.store.workers()].sort((a, b) => Number(b.active) - Number(a.active) || a.name.localeCompare(b.name)),
  );

  protected readonly active = computed(() => this.store.workers().filter((w) => w.active));
  protected readonly terminated = computed(() => this.store.workers().filter((w) => !w.active));

  protected roleLabel(role: WorkerRole): string {
    return WORKER_ROLE_LABEL[role];
  }
}
