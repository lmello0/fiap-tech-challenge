import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/api/api-client';
import { ShopApi } from '../../core/api/shop-api';
import type { AppointmentInfoDto } from '../../core/api/dto';
import { Session } from '../../core/auth/session';
import { Callout } from '../../shared/ui/callout';
import { Icon } from '../../shared/ui/icon';

/**
 * `/appointments/pickup/book?token=…` — the car is ready; pick a time.
 *
 * The smallest of the three returned cards: one decision, no form. The
 * invitation token carries the work order, the customer and the vehicle, so
 * this page never asks which car — the shop already knows, and asking would
 * invite an answer it would then ignore.
 *
 * Two things it does not do, deliberately:
 *
 * - **It does not book on arrival.** The token is single-use and consumed by
 *   the booking call, so a mail scanner following the link would otherwise
 *   burn the invitation and pick a slot nobody chose.
 * - **It does not serve a signed-in customer.** They already have a better
 *   route — `/my/booking?pickup=` reads their real job and vehicle list — and
 *   spending a single-use invitation on someone holding a session is a waste
 *   of a token they never needed. They are redirected there instead.
 */
@Component({
  selector: 'app-book-pickup',
  imports: [Callout, Icon, RouterLink],
  templateUrl: './book-pickup.html',
  styleUrls: ['./guest.scss', './book-pickup.scss'],
})
export class BookPickup implements OnInit {
  private readonly api = inject(ShopApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly session = inject(Session);

  private readonly token = this.route.snapshot.queryParamMap.get('token');

  protected readonly missingToken = !this.token;

  protected readonly date = signal('');
  protected readonly slot = signal('');
  protected readonly slots = signal<readonly string[]>([]);
  protected readonly slotsBusy = signal(false);
  protected readonly slotsAsked = signal(false);

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly spent = signal(false);
  protected readonly booked = signal<AppointmentInfoDto | null>(null);

  protected readonly minDate = toDateInput(new Date());

  protected readonly weekendPicked = computed(() => isWeekend(this.date()));

  async ngOnInit(): Promise<void> {
    // A customer with a session has a better door, and it costs no token.
    if (this.session.inGarage()) {
      await this.router.navigateByUrl('/my/booking');
    }
  }

  protected async pickDate(value: string): Promise<void> {
    this.date.set(value);
    this.slot.set('');
    this.slots.set([]);
    this.slotsAsked.set(false);
    this.error.set(null);
    if (!value || isWeekend(value)) return;

    this.slotsBusy.set(true);
    try {
      this.slots.set(await this.api.availability('PICKUP', value));
      this.slotsAsked.set(true);
    } catch (error) {
      this.error.set(
        error instanceof ApiError ? error.message : 'Could not read the open slots.',
      );
    } finally {
      this.slotsBusy.set(false);
    }
  }

  protected async book(event: Event): Promise<void> {
    event.preventDefault();
    if (!this.token || !this.slot() || this.busy()) return;

    this.busy.set(true);
    this.error.set(null);
    try {
      this.booked.set(await this.api.bookPickupByToken(this.token, this.slot()));
    } catch (error) {
      // The invitation lasts a fortnight and works once. Spent or expired is
      // the likely failure and reads as its own state, not a slot problem.
      if (error instanceof ApiError && (error.status === 400 || error.status === 404)) {
        this.spent.set(true);
      } else {
        this.error.set(
          error instanceof ApiError ? error.message : 'The pickup could not be booked.',
        );
      }
    } finally {
      this.busy.set(false);
    }
  }

  protected slotLabel(instant: string): string {
    return new Date(instant).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  }

  protected when(instant: string): string {
    return new Date(instant).toLocaleString('en-GB', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}

function toDateInput(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function isWeekend(value: string): boolean {
  if (!value) return false;
  const day = new Date(`${value}T12:00:00`).getDay();
  return day === 0 || day === 6;
}
