import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { ApiError } from '../../core/api/api-client';
import { ShopApi } from '../../core/api/shop-api';
import type { AppointmentInfoDto } from '../../core/api/dto';
import { CUSTOMER_PROCEDURE } from '../../core/domain/customer-procedure';
import { Callout } from '../../shared/ui/callout';
import { Icon } from '../../shared/ui/icon';

type Panel = 'move' | 'cancel' | null;

/**
 * `/appointments/manage?token=…` — the guest's own booking, returned to them.
 *
 * The token in the URL is the whole credential: there is no account behind a
 * guest booking and nothing to sign into. That makes one rule absolute, and it
 * is the backend's rule too — every guest link is POST-consumed precisely so a
 * mail scanner issuing a GET cannot act on it. **Nothing destructive fires on
 * arrival.** The page reads the booking and waits; cancelling is a decision
 * made here, never a consequence of opening the link.
 *
 * ## The reschedule trap
 *
 * `POST /appointments/guest/reschedule` does not move the appointment. It mints
 * a *replacement*, cancels the original as RESCHEDULED, and emails a fresh pair
 * of tokens for the new one. The token this page is holding resolves to the
 * original — and `rescheduleByToken` calls `findOrThrow`, not `resolveLive`, so
 * it does not follow the chain. The moment a move succeeds, the link the
 * customer is looking at manages nothing.
 *
 * So a successful move switches this page into a terminal state: it shows the
 * new booking from the response and says a fresh link is on its way. Leaving
 * the actions live would offer a second move that fails with a 409 nobody
 * could interpret.
 */
@Component({
  selector: 'app-manage-booking',
  imports: [Callout, Icon, RouterLink],
  templateUrl: './manage-booking.html',
  styleUrls: ['./guest.scss', './manage-booking.scss'],
})
export class ManageBooking implements OnInit {
  private readonly api = inject(ShopApi);
  private readonly route = inject(ActivatedRoute);

  protected readonly procedure = CUSTOMER_PROCEDURE;

  private readonly token = this.route.snapshot.queryParamMap.get('token');

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly booking = signal<AppointmentInfoDto | null>(null);

  protected readonly panel = signal<Panel>(null);
  protected readonly busy = signal(false);
  protected readonly actionError = signal<string | null>(null);

  /** Set once a move succeeds — the token this page holds is spent from then on. */
  protected readonly moved = signal(false);

  protected readonly date = signal('');
  protected readonly slot = signal('');
  protected readonly slots = signal<readonly string[]>([]);
  protected readonly slotsBusy = signal(false);
  protected readonly slotsAsked = signal(false);

  protected readonly minDate = toDateInput(new Date());

  protected readonly weekendPicked = computed(() => isWeekend(this.date()));

  /** A booking that can still be moved or cancelled. Everything else is a record. */
  protected readonly live = computed(() => {
    const b = this.booking();
    return b !== null && b.status === 'SCHEDULED' && !this.moved();
  });

  protected readonly inThePast = computed(() => {
    const b = this.booking();
    return b !== null && b.status === 'SCHEDULED' && Date.parse(b.slotStart) < Date.now();
  });

  /**
   * Where this booking sits against the ten-step procedure.
   *
   * A scheduled drop-off has not reached step 1 — the car is not there yet —
   * so nothing is stamped. Checking in is what makes step 1 true, and the
   * appointment records that as `checkedInAt`.
   */
  protected readonly reached = computed(() => (this.booking()?.checkedInAt ? 1 : 0));

  async ngOnInit(): Promise<void> {
    if (!this.token) {
      this.loadError.set('This link is missing its token.');
      this.loading.set(false);
      return;
    }
    try {
      this.booking.set(await this.api.viewGuestBooking(this.token));
    } catch (error) {
      this.loadError.set(
        error instanceof ApiError ? error.message : 'This link is invalid or has expired.',
      );
    } finally {
      this.loading.set(false);
    }
  }

  protected open(panel: Panel): void {
    this.panel.set(panel);
    this.actionError.set(null);
  }

  protected async pickDate(value: string): Promise<void> {
    this.date.set(value);
    this.slot.set('');
    this.slots.set([]);
    this.slotsAsked.set(false);
    if (!value || isWeekend(value)) return;

    this.slotsBusy.set(true);
    try {
      const type = this.booking()?.type ?? 'DROPOFF';
      this.slots.set(await this.api.availability(type, value));
      this.slotsAsked.set(true);
    } catch (error) {
      this.actionError.set(
        error instanceof ApiError ? error.message : 'Could not read the open slots.',
      );
    } finally {
      this.slotsBusy.set(false);
    }
  }

  protected async move(): Promise<void> {
    if (!this.token || !this.slot() || this.busy()) return;
    this.busy.set(true);
    this.actionError.set(null);
    try {
      // The response is the *replacement* booking, not the one that moved.
      this.booking.set(await this.api.rescheduleGuestBooking(this.token, this.slot()));
      this.moved.set(true);
      this.panel.set(null);
    } catch (error) {
      this.actionError.set(
        error instanceof ApiError ? error.message : 'The booking could not be moved.',
      );
    } finally {
      this.busy.set(false);
    }
  }

  protected async cancel(): Promise<void> {
    if (!this.token || this.busy()) return;
    this.busy.set(true);
    this.actionError.set(null);
    try {
      // The guest cancel takes no reason, unlike the signed-in customer's.
      this.booking.set(await this.api.cancelGuestBooking(this.token));
      this.panel.set(null);
    } catch (error) {
      this.actionError.set(
        error instanceof ApiError ? error.message : 'The booking could not be cancelled.',
      );
    } finally {
      this.busy.set(false);
    }
  }

  /* --- formatting --------------------------------------------------------- */

  protected when(instant: string): string {
    return new Date(instant).toLocaleString('en-GB', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  protected slotLabel(instant: string): string {
    return new Date(instant).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  }

  protected vehicle(b: AppointmentInfoDto): string {
    const parts = [b.guestVehicleMake, b.guestVehicleModel].filter(Boolean).join(' ');
    return b.guestVehicleYear ? `${parts} · ${b.guestVehicleYear}` : parts || 'Your vehicle';
  }

  /** The registration link is its own token, so it cannot be built from this one. */
  protected readonly registrationHint =
    'The same email carries a second link — the one that finishes setting up an account.';
}

function toDateInput(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function isWeekend(value: string): boolean {
  if (!value) return false;
  const day = new Date(`${value}T12:00:00`).getDay();
  return day === 0 || day === 6;
}
