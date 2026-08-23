import { Component, computed, inject, signal } from '@angular/core';
import { ShopStore } from '../../core/data/shop-store';
import { Session } from '../../core/auth/session';
import {
  APPOINTMENT_CANCEL_REASON_LABEL,
  APPOINTMENT_STATUS_LABEL,
  type AppointmentStatus,
} from '../../core/domain/enums';
import { Callout } from '../../shared/ui/callout';
import { Icon } from '../../shared/ui/icon';

@Component({
  selector: 'app-schedule',
  imports: [Callout, Icon],
  template: `
    <div class="sch">
      <header class="sch__head">
        <div>
          <h1 class="sch__title">Schedule</h1>
          <p class="sch__sub">
            {{ settings().openFrom }}–{{ settings().openTo }}, Monday to Friday ·
            {{ settings().slotMinutes }}-minute slots · drop-off capacity
            {{ settings().dropoffCapacityPerSlot }}, pickup {{ settings().pickupCapacityPerSlot }} per slot
          </p>
        </div>
        <label class="field">
          <span class="sr-only">Filter by status</span>
          <select class="input" [value]="filter()" (change)="onFilter($event)">
            <option value="ALL">All appointments</option>
            @for (s of statuses; track s) {
              <option [value]="s">{{ label(s) }}</option>
            }
          </select>
        </label>
      </header>

      <section class="plate">
        <div class="plate__head">
          <p class="plate__title">5.1 — Booked appointments</p>
          <p class="plate__meta label">{{ rows().length }} listed</p>
        </div>
        <div class="mt-wrap">
          <table class="mt">
            <caption class="sr-only">Appointments with slot, party, vehicle and status.</caption>
            <thead>
              <tr>
                <th scope="col" class="shrink">Slot</th>
                <th scope="col" class="shrink">Type</th>
                <th scope="col">Party</th>
                <th scope="col">Vehicle</th>
                <th scope="col">Complaint</th>
                <th scope="col" class="shrink">Status</th>
                <th scope="col" class="shrink">Action</th>
              </tr>
            </thead>
            <tbody>
              @for (a of rows(); track a.id) {
                <tr>
                  <td class="shrink token sch__slot">{{ slot(a.slotStart) }}</td>
                  <td class="shrink">
                    <span class="sch__type" [class.is-pickup]="a.type === 'PICKUP'">{{ a.type }}</span>
                  </td>
                  <td>
                    @if (a.guestName) {
                      <span class="sch__party">{{ a.guestName }}</span>
                      <span class="sch__guest">Guest — not yet a customer</span>
                    } @else {
                      <span class="sch__party">{{ a.customerName }}</span>
                    }
                  </td>
                  <td>
                    @if (a.vehiclePlate) {
                      <span class="token sch__plate">{{ a.vehiclePlate }}</span>
                      <span class="sch__vehicle">{{ a.vehicleLabel }}</span>
                    } @else {
                      <span class="sch__vehicle"
                        >{{ a.guestVehicleMake }} {{ a.guestVehicleModel }} {{ a.guestVehicleYear }}</span
                      >
                      <span class="sch__guest">Given inline on the booking</span>
                    }
                  </td>
                  <td class="sch__complaint">{{ a.complaint ?? '—' }}</td>
                  <td class="shrink">
                    <span class="sch__status" [class]="'is-' + a.status.toLowerCase()">{{
                      label(a.status)
                    }}</span>
                    @if (a.cancelReason) {
                      <span class="sch__reason">{{ reason(a.cancelReason) }}</span>
                    }
                  </td>
                  <td class="shrink">
                    @if (a.status === 'SCHEDULED' && canAct()) {
                      <button class="btn btn--sm btn--primary" type="button" (click)="checkIn(a.id)">
                        Check in
                      </button>
                    } @else if (a.status === 'COMPLETED') {
                      <span class="sch__done"
                        ><app-icon name="check" [size]="12" /> {{ time(a.checkedInAt!) }}</span
                      >
                    } @else {
                      <span class="sch__inert">—</span>
                    }
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="7">
                    <div class="empty">
                      <p class="empty__title">Nothing booked in this view</p>
                      <p>Change the status filter to see appointments outside it.</p>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>

      <section class="plate sch__closures">
        <div class="plate__head">
          <p class="plate__title">5.2 — Closures</p>
          <p class="plate__meta label">Manager only</p>
        </div>
        <div class="plate__body">
          <app-callout tier="caution">
            Closing a date that already has appointments cancels them and notifies the affected
            customers and guests. A closure is a one-off decision, not a recurring holiday.
          </app-callout>

          <table class="mt sch__closure-table">
            <caption class="sr-only">Dates the shop is closed.</caption>
            <thead>
              <tr>
                <th scope="col" class="shrink">Date</th>
                <th scope="col">Message to customers</th>
                <th scope="col" class="r shrink">Cancelled</th>
              </tr>
            </thead>
            <tbody>
              @for (c of closures(); track c.date) {
                <tr>
                  <td class="shrink token">{{ c.date }}</td>
                  <td>{{ c.message ?? 'No explanation given.' }}</td>
                  <td class="r num">
                    @if (c.cancelledAppointments > 0) {
                      <span class="sch__cancelled">{{ c.cancelledAppointments }}</span>
                    } @else {
                      0
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

    .sch__head {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 2rem;
      flex-wrap: wrap;
      margin-bottom: 1.25rem;
    }

    .sch__title {
      font-size: var(--step-4);
      font-weight: 700;
      letter-spacing: -0.02em;
      line-height: 1;
    }

    .sch__sub {
      margin-top: 0.35rem;
      color: var(--ink-2);
    }

    .sch__slot {
      white-space: nowrap;
      font-weight: 600;
    }

    .sch__type {
      font-family: var(--face-cond);
      font-size: 0.66rem;
      font-weight: 700;
      letter-spacing: 0.1em;
      border: 1px solid var(--rule-strong);
      padding: 0.05rem 0.3rem;
      color: var(--ink-2);
    }

    .sch__type.is-pickup {
      color: var(--sec-schedule);
      border-color: var(--sec-schedule);
    }

    .sch__party {
      display: block;
    }

    .sch__guest {
      display: block;
      font-size: 0.72rem;
      color: var(--caution-ink);
    }

    .sch__plate {
      display: block;
      font-weight: 600;
    }

    .sch__vehicle {
      display: block;
      font-size: 0.78rem;
      color: var(--ink-2);
    }

    .sch__complaint {
      max-width: 26rem;
      color: var(--ink-2);
    }

    .sch__status {
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

    .sch__status.is-completed {
      color: var(--ok);
      background: var(--ok-field);
    }
    .sch__status.is-no_show {
      color: var(--warn);
      background: var(--warn-field);
    }
    .sch__status.is-cancelled {
      color: var(--ink-3);
    }

    .sch__reason {
      display: block;
      margin-top: 0.15rem;
      font-size: 0.7rem;
      color: var(--ink-3);
    }

    .sch__done {
      display: inline-flex;
      align-items: center;
      gap: 0.25rem;
      color: var(--ok);
      font-size: 0.78rem;
      white-space: nowrap;
    }

    .sch__inert {
      color: var(--ink-3);
    }

    .sch__closures {
      margin-top: 1.25rem;
    }

    .sch__closure-table {
      margin-top: 1rem;
    }

    .sch__cancelled {
      color: var(--warn);
      font-weight: 600;
    }
  `,
})
export class Schedule {
  private readonly store = inject(ShopStore);
  private readonly session = inject(Session);

  protected readonly statuses: AppointmentStatus[] = ['SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];
  protected readonly filter = signal<AppointmentStatus | 'ALL'>('ALL');

  protected readonly settings = this.store.settings;
  protected readonly closures = this.store.closures;

  protected readonly rows = computed(() => {
    const f = this.filter();
    return this.store
      .appointments()
      .filter((a) => (f === 'ALL' ? true : a.status === f))
      .sort((a, b) => a.slotStart.localeCompare(b.slotStart));
  });

  protected readonly canAct = computed(() => this.session.hasAnyRole('ATTENDANT', 'MANAGER'));

  protected checkIn(id: string): void {
    this.store.checkIn(id);
  }

  protected onFilter(event: Event): void {
    this.filter.set((event.target as HTMLSelectElement).value as AppointmentStatus | 'ALL');
  }

  protected label(status: AppointmentStatus): string {
    return APPOINTMENT_STATUS_LABEL[status];
  }

  protected reason(r: keyof typeof APPOINTMENT_CANCEL_REASON_LABEL): string {
    return APPOINTMENT_CANCEL_REASON_LABEL[r];
  }

  protected slot(iso: string): string {
    return new Date(iso).toLocaleString('en-GB', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  protected time(iso: string): string {
    return new Date(iso).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  }
}
