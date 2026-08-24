import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiError } from '../../core/api/api-client';
import { CustomerStore } from '../../core/customer/customer-store';
import { Callout } from '../../shared/ui/callout';
import type { Appointment } from '../../core/domain/models';

type Mode = 'DROPOFF' | 'PICKUP';

/**
 * Booking a visit, and managing the ones already booked.
 *
 * A drop-off is booked against one of the customer's own vehicles; a pickup is
 * booked against a job that is already waiting to be collected, which is why
 * this screen is reached from a job with `?pickup=<id>` rather than offering a
 * free choice the API would refuse.
 *
 * The bookings register below has the same honesty as the jobs one: the API
 * gives a customer no way to read their own appointments back — only to cancel
 * or move one by id — so this is what this browser has booked, stated as such.
 * Filed in `docs/backend-requirements.md` alongside the jobs list.
 */
@Component({
  selector: 'app-my-booking',
  imports: [Callout, RouterLink],
  templateUrl: './my-booking.html',
  styleUrls: ['./garage.scss', './my-booking.scss'],
})
export class MyBooking {
  protected readonly store = inject(CustomerStore);
  private readonly route = inject(ActivatedRoute);

  /** A job id in `?pickup=` puts this screen in pickup mode for that job. */
  protected readonly pickupJobId = this.route.snapshot.queryParamMap.get('pickup');
  protected readonly mode: Mode = this.pickupJobId ? 'PICKUP' : 'DROPOFF';

  protected readonly vehicleId = signal('');
  protected readonly complaint = signal('');
  protected readonly date = signal('');
  protected readonly slot = signal('');

  protected readonly slots = signal<readonly string[]>([]);
  protected readonly slotsBusy = signal(false);
  protected readonly slotsAsked = signal(false);
  protected readonly slotsError = signal<string | null>(null);

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly booked = signal<Appointment | null>(null);

  /** A booking being cancelled or moved, and which. */
  protected readonly acting = signal<{ id: string; kind: 'cancel' | 'move' } | null>(null);
  protected readonly actingBusy = signal(false);
  protected readonly actingError = signal<string | null>(null);
  protected readonly moveDate = signal('');
  protected readonly moveSlots = signal<readonly string[]>([]);
  protected readonly moveSlot = signal('');
  protected readonly cancelMessage = signal('');

  protected readonly minDate = toDateInput(new Date());

  protected readonly job = computed(() =>
    this.pickupJobId ? (this.store.job(this.pickupJobId) ?? null) : null,
  );

  protected readonly weekendPicked = computed(() => isWeekend(this.date()));

  protected readonly canBook = computed(() => {
    if (!this.slot()) return false;
    if (this.mode === 'PICKUP') return this.job() !== null && this.vehicleId().length > 0;
    return this.vehicleId().length > 0 && this.complaint().trim().length > 0;
  });

  protected async pickDate(value: string): Promise<void> {
    this.date.set(value);
    this.slot.set('');
    this.slots.set([]);
    this.slotsError.set(null);
    this.slotsAsked.set(false);
    if (!value || isWeekend(value)) return;

    this.slotsBusy.set(true);
    try {
      this.slots.set(await this.store.slots(this.mode, value));
      this.slotsAsked.set(true);
    } catch (error) {
      this.slotsError.set(
        error instanceof ApiError ? error.message : 'Could not read the open slots.',
      );
    } finally {
      this.slotsBusy.set(false);
    }
  }

  protected async book(event: Event): Promise<void> {
    event.preventDefault();
    if (!this.canBook() || this.busy()) return;

    this.busy.set(true);
    this.error.set(null);
    try {
      const appointment =
        this.mode === 'PICKUP'
          ? await this.store.bookPickup(this.pickupJobId!, this.vehicleId(), this.slot())
          : await this.store.bookDropoff({
              vehicleId: this.vehicleId(),
              complaint: this.complaint().trim(),
              slotStart: this.slot(),
            });
      this.booked.set(appointment);
      this.complaint.set('');
      this.slot.set('');
      this.slots.set([]);
      this.slotsAsked.set(false);
      this.date.set('');
    } catch (error) {
      this.error.set(
        error instanceof ApiError ? error.message : 'The booking could not be made.',
      );
    } finally {
      this.busy.set(false);
    }
  }

  /* --- managing what is already booked ------------------------------------ */

  protected startAction(id: string, kind: 'cancel' | 'move'): void {
    this.acting.set({ id, kind });
    this.actingError.set(null);
    this.moveDate.set('');
    this.moveSlots.set([]);
    this.moveSlot.set('');
    this.cancelMessage.set('');
  }

  protected async pickMoveDate(value: string, type: Mode): Promise<void> {
    this.moveDate.set(value);
    this.moveSlot.set('');
    this.moveSlots.set([]);
    if (!value || isWeekend(value)) return;
    try {
      this.moveSlots.set(await this.store.slots(type, value));
    } catch (error) {
      this.actingError.set(
        error instanceof ApiError ? error.message : 'Could not read the open slots.',
      );
    }
  }

  protected async confirmCancel(id: string): Promise<void> {
    this.actingBusy.set(true);
    this.actingError.set(null);
    try {
      await this.store.cancelBooking(id, this.cancelMessage());
      this.acting.set(null);
    } catch (error) {
      this.actingError.set(
        error instanceof ApiError ? error.message : 'The booking was not cancelled.',
      );
    } finally {
      this.actingBusy.set(false);
    }
  }

  protected async confirmMove(id: string): Promise<void> {
    if (!this.moveSlot()) return;
    this.actingBusy.set(true);
    this.actingError.set(null);
    try {
      await this.store.rescheduleBooking(id, this.moveSlot());
      this.acting.set(null);
    } catch (error) {
      this.actingError.set(
        error instanceof ApiError ? error.message : 'The booking was not moved.',
      );
    } finally {
      this.actingBusy.set(false);
    }
  }

  /* --- formatting --------------------------------------------------------- */

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

  protected vehicleLabel(id: string | null): string {
    if (!id) return '—';
    const vehicle = this.store.vehicle(id);
    return vehicle ? `${vehicle.make} ${vehicle.model} · ${vehicle.licensePlate}` : 'Your vehicle';
  }

  protected isActing(id: string, kind: 'cancel' | 'move'): boolean {
    const acting = this.acting();
    return acting?.id === id && acting.kind === kind;
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
