import { Component, computed, inject, signal } from '@angular/core';
import { ShopStore } from '../../core/data/shop-store';
import { Session } from '../../core/auth/session';
import { PURCHASE_ORDER_STATUS_LABEL, STOCK_MOVEMENT_LABEL, UOM_ABBR } from '../../core/domain/enums';
import { Callout } from '../../shared/ui/callout';
import { Icon } from '../../shared/ui/icon';

type Tab = 'parts' | 'purchasing' | 'ledger';

@Component({
  selector: 'app-inventory',
  imports: [Callout, Icon],
  template: `
    <div class="inv">
      <header class="inv__head">
        <div>
          <h1 class="inv__title">Inventory</h1>
          <p class="inv__sub">
            {{ parts().length }} catalogue lines · {{ belowMin().length }} at or below reorder point ·
            {{ negative().length }} oversubscribed
          </p>
        </div>
        @if (readOnly()) {
          <p class="inv__readonly">
            <app-icon name="lock" [size]="13" /> Read-only — a mechanic may look up parts and stock but
            not change them
          </p>
        }
      </header>

      <nav class="inv__tabs" aria-label="Inventory views">
        @for (t of tabs; track t.id) {
          <button
            class="inv__tab"
            type="button"
            [class.is-active]="tab() === t.id"
            [attr.aria-current]="tab() === t.id ? 'page' : null"
            (click)="tab.set(t.id)"
          >
            {{ t.label }}
          </button>
        }
      </nav>

      @switch (tab()) {
        @case ('parts') {
          <section class="plate">
            <div class="plate__head">
              <p class="plate__title">6.1 — Part catalogue and stock position</p>
              <p class="plate__meta label">Available = on hand − reserved</p>
            </div>
            <div class="mt-wrap">
              <table class="mt">
                <caption class="sr-only">Parts with price, cost and stock position.</caption>
                <thead>
                  <tr>
                    <th scope="col" class="shrink">SKU</th>
                    <th scope="col">Part</th>
                    <th scope="col">Brand</th>
                    <th scope="col" class="r shrink">On hand</th>
                    <th scope="col" class="r shrink">Reserved</th>
                    <th scope="col" class="r shrink">Available</th>
                    <th scope="col" class="r shrink">Sale</th>
                    <th scope="col" class="r shrink">Avg cost</th>
                    <th scope="col" class="shrink">Reorder</th>
                  </tr>
                </thead>
                <tbody>
                  @for (p of parts(); track p.id) {
                    <tr [class.is-inactive]="!p.active">
                      <td class="shrink token">{{ p.sku }}</td>
                      <td>
                        {{ p.name }}
                        @if (!p.active) {
                          <span class="inv__flag">withdrawn</span>
                        }
                      </td>
                      <td class="inv__brand">{{ p.brand ?? '—' }}</td>
                      <td class="r num">{{ p.quantityOnHand }} {{ uom(p.unitOfMeasure) }}</td>
                      <td class="r num">{{ p.quantityReserved }}</td>
                      <td class="r num">
                        <span [class.inv__short]="p.available < 0" [class.inv__low]="p.available >= 0 && isLow(p.id, p.available)">{{
                          p.available
                        }}</span>
                      </td>
                      <td class="r num">{{ money(p.salePrice) }}</td>
                      <td class="r num inv__cost">{{ money(p.averageCost) }}</td>
                      <td class="shrink">
                        @if (rule(p.id); as r) {
                          <span class="inv__rule" [class.is-off]="!r.enabled">
                            {{ r.min }}→{{ r.max }}
                          </span>
                        } @else {
                          <span class="inv__norule">none</span>
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </section>

          @if (negative().length) {
            <div class="inv__notice">
              <app-callout tier="warning">
                <p class="inv__notice-lead">
                  {{ negative().length }}
                  {{ negative().length === 1 ? 'part is' : 'parts are' }} reserved beyond what is on
                  the shelf.
                </p>
                <p>
                  Every work order holding one of these reservations is blocked from starting service
                  until stock is received. Nothing has been consumed.
                </p>
              </app-callout>
            </div>
          }
        }

        @case ('purchasing') {
          <section class="plate">
            <div class="plate__head">
              <p class="plate__title">6.2 — Purchase orders</p>
              <p class="plate__meta label">Vendor purchasing is mocked — there is no live vendor API</p>
            </div>
            <div class="mt-wrap">
              <table class="mt">
                <caption class="sr-only">Purchase orders and their receipt progress.</caption>
                <thead>
                  <tr>
                    <th scope="col" class="shrink">Order</th>
                    <th scope="col">Vendor</th>
                    <th scope="col">Lines</th>
                    <th scope="col" class="shrink">Placed</th>
                    <th scope="col" class="shrink">Expected</th>
                    <th scope="col" class="shrink">Status</th>
                  </tr>
                </thead>
                <tbody>
                  @for (po of purchaseOrders(); track po.id) {
                    <tr>
                      <td class="shrink token">{{ po.code }}</td>
                      <td>{{ po.vendorName }}</td>
                      <td>
                        @for (l of po.lines; track l.partId) {
                          <span class="inv__poline">
                            <span class="token">{{ l.sku }}</span> {{ l.partName }} —
                            <span class="num">{{ l.received }}</span> of
                            <span class="num">{{ l.quantity }}</span> received
                          </span>
                        }
                      </td>
                      <td class="shrink">{{ date(po.placedAt) }}</td>
                      <td class="shrink">{{ po.expectedAt ? date(po.expectedAt) : '—' }}</td>
                      <td class="shrink">
                        <span class="inv__status" [class]="'is-' + po.status.toLowerCase()">{{
                          poLabel(po.status)
                        }}</span>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </section>
        }

        @case ('ledger') {
          <section class="plate">
            <div class="plate__head">
              <p class="plate__title">6.3 — Stock movements</p>
              <p class="plate__meta label">Append-only — reservations are not movements</p>
            </div>
            <div class="mt-wrap">
              <table class="mt">
                <caption class="sr-only">Every real change in a part's quantity on hand.</caption>
                <thead>
                  <tr>
                    <th scope="col" class="shrink">When</th>
                    <th scope="col" class="shrink">Type</th>
                    <th scope="col">Part</th>
                    <th scope="col" class="r shrink">Qty</th>
                    <th scope="col" class="r shrink">Unit cost</th>
                    <th scope="col">Against</th>
                  </tr>
                </thead>
                <tbody>
                  @for (m of movements(); track m.id) {
                    <tr>
                      <td class="shrink">{{ date(m.occurredAt) }}</td>
                      <td class="shrink">
                        <span class="inv__mv" [class]="'is-' + m.type.toLowerCase()">{{
                          mvLabel(m.type)
                        }}</span>
                      </td>
                      <td>
                        <span class="token">{{ partSku(m.partId) }}</span> {{ partName(m.partId) }}
                      </td>
                      <td class="r num" [class.inv__short]="m.quantity < 0">
                        {{ m.quantity > 0 ? '+' : '' }}{{ m.quantity }}
                      </td>
                      <td class="r num inv__cost">{{ m.unitCost ? money(m.unitCost) : '—' }}</td>
                      <td>
                        @if (m.referenceLabel) {
                          <span class="token">{{ m.referenceLabel }}</span>
                        } @else if (m.reason) {
                          <span class="inv__reason">{{ m.reason }}</span>
                        } @else {
                          —
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </section>
        }
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
    }

    .plate {
      border-top: 3px solid var(--section-ink, var(--ink));
    }

    .plate__meta {
      color: var(--ink-3);
    }

    .inv__head {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 2rem;
      flex-wrap: wrap;
      margin-bottom: 1rem;
    }

    .inv__title {
      font-size: var(--step-4);
      font-weight: 700;
      letter-spacing: -0.02em;
      line-height: 1;
    }

    .inv__sub {
      margin-top: 0.35rem;
      color: var(--ink-2);
    }

    .inv__readonly {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      font-size: var(--step--1);
      color: var(--ink-3);
      border: 1px dashed var(--rule-strong);
      padding: 0.3rem 0.55rem;
    }

    /* section sub-tabs, ruled rather than pilled */
    .inv__tabs {
      display: flex;
      gap: 0;
      margin-bottom: 1.25rem;
      border-bottom: 1px solid var(--ink);
    }

    .inv__tab {
      background: transparent;
      border: 1px solid var(--rule);
      border-bottom: 0;
      margin-bottom: -1px;
      padding: 0.4rem 0.9rem;
      font-family: var(--face-cond);
      font-size: var(--step-0);
      font-weight: 600;
      letter-spacing: 0.05em;
      text-transform: uppercase;
      color: var(--ink-3);
      cursor: pointer;
    }

    .inv__tab + .inv__tab {
      margin-left: -1px;
    }

    .inv__tab:hover {
      color: var(--ink);
    }

    .inv__tab.is-active {
      background: var(--plate);
      border-color: var(--ink);
      color: var(--ink);
    }

    .inv__brand,
    .inv__cost {
      color: var(--ink-2);
    }

    .inv__short {
      color: var(--warn);
      font-weight: 600;
    }

    .inv__low {
      color: var(--caution-ink);
      font-weight: 600;
    }

    .inv__flag {
      font-family: var(--face-cond);
      font-size: 0.64rem;
      font-weight: 700;
      letter-spacing: 0.09em;
      text-transform: uppercase;
      color: var(--ink-3);
      border: 1px solid var(--rule-strong);
      padding: 0 0.25rem;
      margin-left: 0.35rem;
    }

    tr.is-inactive td {
      color: var(--ink-3);
    }

    .inv__rule {
      font-family: var(--face-mono);
      font-size: 0.72rem;
      color: var(--ink-2);
    }

    .inv__rule.is-off {
      text-decoration: line-through;
      color: var(--ink-3);
    }

    .inv__norule {
      font-size: 0.72rem;
      color: var(--ink-3);
    }

    .inv__notice {
      margin-top: 1.25rem;
    }

    .inv__notice-lead {
      font-weight: 600;
    }

    .inv__poline {
      display: block;
      font-size: 0.8rem;
    }

    .inv__status,
    .inv__mv {
      font-family: var(--face-cond);
      font-size: var(--step--1);
      font-weight: 600;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      border: 1px solid currentColor;
      padding: 0.05rem 0.35rem;
      white-space: nowrap;
      color: var(--ink-2);
    }

    .inv__status.is-received,
    .inv__mv.is-purchase {
      color: var(--ok);
      background: var(--ok-field);
    }
    .inv__status.is-partially_received {
      color: var(--caution-ink);
      background: var(--caution-field);
    }
    .inv__status.is-cancelled {
      color: var(--ink-3);
    }
    .inv__status.is-placed {
      color: var(--ref);
      background: var(--ref-field);
    }
    .inv__mv.is-consumption {
      color: var(--ref);
      background: var(--ref-field);
    }
    .inv__mv.is-adjustment {
      color: var(--caution-ink);
      background: var(--caution-field);
    }

    .inv__reason {
      font-style: italic;
      color: var(--ink-2);
      font-size: 0.82rem;
    }
  `,
})
export class Inventory {
  private readonly store = inject(ShopStore);
  private readonly session = inject(Session);

  protected readonly tabs: { id: Tab; label: string }[] = [
    { id: 'parts', label: 'Parts & stock' },
    { id: 'purchasing', label: 'Purchasing' },
    { id: 'ledger', label: 'Movements' },
  ];

  protected readonly tab = signal<Tab>('parts');

  protected readonly parts = this.store.parts;
  protected readonly purchaseOrders = this.store.purchaseOrders;
  protected readonly movements = this.store.movements;

  /** A mechanic may read the catalogue but not write to it. */
  protected readonly readOnly = computed(() => this.session.role() === 'MECHANIC');

  protected readonly negative = computed(() => this.store.parts().filter((p) => p.available < 0));

  protected readonly belowMin = computed(() =>
    this.store.parts().filter((p) => {
      const r = this.rule(p.id);
      return r?.enabled ? p.available <= r.min : false;
    }),
  );

  protected rule(partId: string) {
    return this.store.reorderRules().find((r) => r.partId === partId);
  }

  protected isLow(partId: string, available: number): boolean {
    const r = this.rule(partId);
    return !!r?.enabled && available <= r.min;
  }

  protected partSku(partId: string): string {
    return this.store.part(partId)?.sku ?? '—';
  }

  protected partName(partId: string): string {
    return this.store.part(partId)?.name ?? 'Unknown part';
  }

  protected uom(u: keyof typeof UOM_ABBR): string {
    return UOM_ABBR[u];
  }

  protected poLabel(s: keyof typeof PURCHASE_ORDER_STATUS_LABEL): string {
    return PURCHASE_ORDER_STATUS_LABEL[s];
  }

  protected mvLabel(t: keyof typeof STOCK_MOVEMENT_LABEL): string {
    return STOCK_MOVEMENT_LABEL[t];
  }

  protected money(v: number): string {
    return v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  protected date(iso: string): string {
    return new Date(iso).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: '2-digit' });
  }
}
