import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/api/api-client';
import { ShopApi } from '../../core/api/shop-api';
import { Session } from '../../core/auth/session';
import { CUSTOMER_PROCEDURE } from '../../core/domain/customer-procedure';
import { Callout } from '../../shared/ui/callout';

/**
 * The shop's front door, composed as the thing a shop actually hands you: a
 * blank repair order.
 *
 * The marketing surface *is* the working document. There is no hero above it
 * arguing for the shop and no card grid of benefits underneath — the offer is
 * legible because the form is legible, and the routing margin down the right
 * edge prints the whole ten-step procedure the job will travel, so the visitor
 * can see the entire road before filling in a single blank.
 *
 * Everything on this page is real. `POST /appointments/dropoff/guest` is a
 * public endpoint and `GET /appointments/availability` needs no token, so a
 * walk-in can book a slot without an account — which is the shop's own
 * position, not a shortcut. Signing in or registering is offered at the point
 * of filing, where it buys something (the job follows you, and you approve the
 * price from your own account), rather than as a wall in front of the form.
 */
@Component({
  selector: 'app-landing',
  imports: [Callout, DatePipe, RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
})
export class Landing {
  private readonly api = inject(ShopApi);
  private readonly router = inject(Router);
  protected readonly session = inject(Session);

  /** The procedure, in the customer's reading. Printed down the routing margin. */
  protected readonly procedure = CUSTOMER_PROCEDURE;

  protected readonly today = new Date();

  /* --- the blanks -------------------------------------------------------- */

  protected readonly name = signal('');
  protected readonly phone = signal('');
  protected readonly email = signal('');
  protected readonly make = signal('');
  protected readonly model = signal('');
  protected readonly year = signal('');
  protected readonly complaint = signal('');
  protected readonly date = signal('');
  protected readonly slot = signal('');

  protected readonly touched = signal(false);

  /* --- slots ------------------------------------------------------------- */

  protected readonly slots = signal<readonly string[]>([]);
  protected readonly slotsBusy = signal(false);
  protected readonly slotsError = signal<string | null>(null);
  /** Set once a date has been asked about, so "none left" can differ from "not asked". */
  protected readonly slotsAsked = signal(false);

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly filed = signal<{ slotStart: string; email: string } | null>(null);

  /** The shop is open weekdays; a weekend date has no slots to fetch. */
  protected readonly minDate = toDateInput(new Date());

  protected readonly weekendPicked = computed(() => {
    const value = this.date();
    if (!value) return false;
    const day = new Date(`${value}T12:00:00`).getDay();
    return day === 0 || day === 6;
  });

  protected readonly yearNumber = computed(() => {
    const parsed = Number.parseInt(this.year(), 10);
    return Number.isFinite(parsed) ? parsed : null;
  });

  protected readonly yearValid = computed(() => {
    const value = this.yearNumber();
    return value !== null && value >= 1900 && value <= this.today.getFullYear() + 1;
  });

  protected readonly complete = computed(
    () =>
      this.name().trim().length > 0 &&
      this.phone().trim().length > 0 &&
      isEmail(this.email()) &&
      this.make().trim().length > 0 &&
      this.model().trim().length > 0 &&
      this.yearValid() &&
      this.complaint().trim().length > 0 &&
      this.slot().length > 0,
  );

  /** Named so the filing band can say what is still blank, rather than just refusing. */
  protected readonly missing = computed(() => {
    const gaps: string[] = [];
    if (!this.name().trim()) gaps.push('your name');
    if (!this.phone().trim()) gaps.push('a phone number');
    if (!isEmail(this.email())) gaps.push('a valid email');
    if (!this.make().trim() || !this.model().trim()) gaps.push('the make and model');
    if (!this.yearValid()) gaps.push('the year');
    if (!this.complaint().trim()) gaps.push('what is wrong');
    if (!this.slot()) gaps.push('a drop-off slot');
    return gaps;
  });

  protected async pickDate(value: string): Promise<void> {
    this.date.set(value);
    this.slot.set('');
    this.slots.set([]);
    this.slotsError.set(null);
    this.slotsAsked.set(false);
    if (!value || this.weekendPicked()) return;

    this.slotsBusy.set(true);
    try {
      const open = await this.api.availability('DROPOFF', value);
      this.slots.set(open);
      this.slotsAsked.set(true);
    } catch (error) {
      this.slotsError.set(
        error instanceof ApiError ? error.message : 'Could not read the open slots.',
      );
    } finally {
      this.slotsBusy.set(false);
    }
  }

  protected slotLabel(instant: string): string {
    return new Date(instant).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  }

  protected longSlot(instant: string): string {
    return new Date(instant).toLocaleString('en-GB', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  /**
   * File the card as a guest.
   *
   * The account offer sits beside this button rather than in front of it: a
   * booking made here is a real appointment, and the guest is converted to a
   * customer either by completing registration from the email or by the
   * attendant at check-in. Nothing is lost by not registering now.
   */
  protected async fileAsGuest(): Promise<void> {
    this.touched.set(true);
    if (!this.complete() || this.busy()) return;

    this.busy.set(true);
    this.error.set(null);
    try {
      const appointment = await this.api.bookGuestDropoff({
        guestName: this.name().trim(),
        guestPhone: this.phone().trim(),
        guestEmail: this.email().trim(),
        guestVehicleMake: this.make().trim(),
        guestVehicleModel: this.model().trim(),
        guestVehicleYear: this.yearNumber()!,
        complaint: this.complaint().trim(),
        slotStart: this.slot(),
      });
      this.filed.set({ slotStart: appointment.slotStart, email: this.email().trim() });
    } catch (error) {
      this.error.set(
        error instanceof ApiError ? error.message : 'The booking could not be filed.',
      );
    } finally {
      this.busy.set(false);
    }
  }

  /**
   * Carry the card over to registration rather than making them type it twice.
   *
   * Only the fields registration can actually use travel — the vehicle and the
   * complaint are not part of `CreateUserCommand`, and pretending otherwise
   * would drop them silently on the far side.
   */
  protected async register(): Promise<void> {
    await this.router.navigate(['/register'], {
      state: {
        name: this.name().trim(),
        phone: this.phone().trim(),
        email: this.email().trim(),
      },
    });
  }
}

function toDateInput(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function isEmail(value: string): boolean {
  const trimmed = value.trim();
  return trimmed.length > 3 && trimmed.includes('@') && !trimmed.startsWith('@') && !trimmed.endsWith('@');
}
