import { Component, computed, inject, signal } from '@angular/core';
import { ShopStore } from '../../core/data/shop-store';
import { Callout } from '../../shared/ui/callout';

@Component({
  selector: 'app-customers',
  imports: [Callout],
  template: `
    <div class="rec">
      <header class="rec__head">
        <div>
          <h1 class="rec__title">Customers</h1>
          <p class="rec__sub">
            {{ rows().length }} on file · {{ deactivated().length }} deactivated
          </p>
        </div>
        <label class="field">
          <span class="sr-only">Search customers</span>
          <input
            class="input rec__search"
            type="search"
            placeholder="Name, email or document"
            [value]="q()"
            (input)="onQuery($event)"
          />
        </label>
      </header>

      <div class="rec__notice">
        <app-callout tier="note">
          Deactivation is a reversible soft-delete of the customer facet only. It never touches a
          worker facet the same user might also hold — someone can be a deactivated customer and an
          active mechanic at once.
        </app-callout>
      </div>

      <section class="plate">
        <div class="plate__head">
          <p class="plate__title">7.1 — Customer accounts</p>
          <p class="plate__meta label">Attendant or manager</p>
        </div>
        <div class="mt-wrap">
          <table class="mt">
            <caption class="sr-only">Customers with contact details and facet state.</caption>
            <thead>
              <tr>
                <th scope="col">Name</th>
                <th scope="col">Document</th>
                <th scope="col">Email</th>
                <th scope="col">Phone</th>
                <th scope="col" class="r shrink">Vehicles</th>
                <th scope="col" class="shrink">Since</th>
                <th scope="col" class="shrink">Facet</th>
              </tr>
            </thead>
            <tbody>
              @for (c of rows(); track c.id) {
                <tr [class.is-inactive]="!c.active">
                  <td class="rec__name">{{ c.name }}</td>
                  <td>
                    <span class="token">{{ c.document }}</span>
                    <span class="rec__doctype">{{ c.documentType }}</span>
                  </td>
                  <td class="rec__muted">{{ c.email }}</td>
                  <td class="rec__muted">{{ c.phone ?? '—' }}</td>
                  <td class="r num">{{ c.vehicleCount ?? 0 }}</td>
                  <td class="shrink rec__muted">{{ c.createdAt }}</td>
                  <td class="shrink">
                    <span class="rec__state" [class.is-off]="!c.active">{{
                      c.active ? 'Active' : 'Deactivated'
                    }}</span>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="7">
                    <div class="empty">
                      <p class="empty__title">No customer matches that search</p>
                      <p>Try a partial name, an email, or the document without punctuation.</p>
                    </div>
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
export class Customers {
  private readonly store = inject(ShopStore);
  protected readonly q = signal('');

  protected readonly rows = computed(() => {
    const q = this.q().trim().toLowerCase();
    return this.store
      .customers()
      .filter((c) =>
        q ? [c.name, c.email, c.document].some((v) => v.toLowerCase().includes(q)) : true,
      );
  });

  protected readonly deactivated = computed(() => this.store.customers().filter((c) => !c.active));

  protected onQuery(event: Event): void {
    this.q.set((event.target as HTMLInputElement).value);
  }
}
