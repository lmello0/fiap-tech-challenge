import { Component, computed, inject, signal } from '@angular/core';
import {
  FormField,
  form,
  min,
  minLength,
  required,
  schema,
  validate,
} from '@angular/forms/signals';
import { ShopStore } from '../../core/data/shop-store';
import { Session } from '../../core/auth/session';
import { EntryBand } from '../../shared/ui/entry-band';
import { FormFieldRow } from '../../shared/ui/form-field';
import type { Appointment, Closure } from '../../core/domain/models';
import {
  APPOINTMENT_CANCEL_REASON_LABEL,
  APPOINTMENT_STATUS_LABEL,
  type AppointmentStatus,
} from '../../core/domain/enums';
import { Callout } from '../../shared/ui/callout';
import { Icon } from '../../shared/ui/icon';

interface SettingsDraft {
  businessStartTime: string;
  businessEndTime: string;
  dropoffSlotCapacity: number | null;
  pickupSlotCapacity: number | null;
}

const settingsSchema = schema<SettingsDraft>((p) => {
  required(p.businessStartTime, { message: 'An opening time is required.' });
  required(p.businessEndTime, { message: 'A closing time is required.' });
  validate(p.businessEndTime, (ctx) => {
    const end = ctx.value();
    const start = ctx.valueOf(p.businessStartTime);
    if (!end || !start) return null;
    return end > start ? null : { kind: 'hours', message: 'The shop must close after it opens.' };
  });
  min(p.dropoffSlotCapacity, 1, { message: 'At least one drop-off per slot.' });
  min(p.pickupSlotCapacity, 1, { message: 'At least one pickup per slot.' });
});

interface ClosureDraft {
  date: string;
  message: string;
}

const closureSchema = schema<ClosureDraft>((p) => {
  required(p.date, { message: 'Which date is closed?' });
});

/**
 * A staff booking is one of two things, and the form is honest about which:
 * either an existing customer and their vehicle, or a walk-in whose details are
 * taken inline and who becomes a Customer later, at check-in.
 */
interface BookingDraft {
  forGuest: boolean;
  customerId: string;
  vehicleId: string;
  guestName: string;
  guestPhone: string;
  guestEmail: string;
  guestVehicleMake: string;
  guestVehicleModel: string;
  guestVehicleYear: number | null;
  complaint: string;
  date: string;
  slotStart: string;
}

const bookingSchema = schema<BookingDraft>((p) => {
  required(p.complaint, { message: 'Record what the customer reported.' });
  minLength(p.complaint, 5, { message: 'A sentence, in their own terms.' });
  required(p.date, { message: 'Pick a date.' });
  required(p.slotStart, { message: 'Pick a slot.' });
  validate(p.customerId, (ctx) =>
    !ctx.valueOf(p.forGuest) && !ctx.value()
      ? { kind: 'who', message: 'Which customer is bringing the vehicle in?' }
      : null,
  );
  validate(p.vehicleId, (ctx) =>
    !ctx.valueOf(p.forGuest) && !ctx.value()
      ? { kind: 'which', message: 'Which vehicle is coming in?' }
      : null,
  );
  validate(p.guestName, (ctx) =>
    ctx.valueOf(p.forGuest) && !ctx.value().trim()
      ? { kind: 'guest', message: 'A walk-in still needs a name to call.' }
      : null,
  );
  validate(p.guestPhone, (ctx) =>
    ctx.valueOf(p.forGuest) && !ctx.value().trim()
      ? { kind: 'guest', message: 'A number to reach them on.' }
      : null,
  );
  // The API advertises this as optional but cannot complete the booking without
  // it — the booking-management token is emailed to the guest, and a booking
  // with nowhere to send it fails server-side. Required here so it fails in the
  // field instead.
  validate(p.guestEmail, (ctx) =>
    ctx.valueOf(p.forGuest) && !ctx.value().trim()
      ? { kind: 'guest', message: 'Required — the booking link is emailed to them.' }
      : null,
  );
});

@Component({
  selector: 'app-schedule',
  imports: [Callout, Icon, EntryBand, FormFieldRow, FormField],
  templateUrl: './schedule.html',
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

    /* --- entry ---------------------------------------------------------------- */

    .sch__band {
      margin-bottom: 1.25rem;
      border: 1px solid var(--rule-strong);
    }

    .sch__band-row > td {
      padding: 0 !important;
      background: transparent !important;
    }

    .sch__who {
      display: grid;
      gap: 0.3rem;
    }

    .sch__radio {
      display: flex;
      align-items: baseline;
      gap: 0.45rem;
    }

    /* The cost of the decision, stated where the decision is made. */
    .sch__will-cancel {
      color: var(--warn);
    }

    .sch__will-cancel strong {
      color: var(--warn);
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

  /* ---------------------------------------------------------------------
     The calendar: hours, closures, and the booking counter.
     --------------------------------------------------------------------- */

  protected readonly isDemo = this.store.isDemo;
  protected readonly canManage = computed(() => this.session.hasAnyRole('MANAGER'));

  protected readonly editingSettings = signal(false);
  protected readonly addingClosure = signal(false);
  protected readonly removingClosure = signal<string | null>(null);
  protected readonly booking = signal(false);
  protected readonly rescheduling = signal<string | null>(null);
  protected readonly cancelling = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly bandError = signal<string | null>(null);

  protected readonly settingsDraft = signal<SettingsDraft>({
    businessStartTime: '08:00',
    businessEndTime: '18:00',
    dropoffSlotCapacity: 3,
    pickupSlotCapacity: 3,
  });
  protected readonly stf = form(this.settingsDraft, settingsSchema);

  protected readonly closureDraft = signal<ClosureDraft>({ date: '', message: '' });
  protected readonly clf = form(this.closureDraft, closureSchema);

  protected readonly bookingDraft = signal<BookingDraft>(this.emptyBooking());
  protected readonly bf = form(this.bookingDraft, bookingSchema);

  /** Slots still open on the chosen date, read straight from the API. */
  protected readonly slots = signal<string[]>([]);
  protected readonly loadingSlots = signal(false);
  /** The new slot being considered for a reschedule. */
  protected readonly rescheduleTo = signal('');
  protected readonly cancelMessage = signal('');

  private emptyBooking(): BookingDraft {
    return {
      forGuest: false,
      customerId: '',
      vehicleId: '',
      guestName: '',
      guestPhone: '',
      guestEmail: '',
      guestVehicleMake: '',
      guestVehicleModel: '',
      guestVehicleYear: null,
      complaint: '',
      date: new Date().toISOString().slice(0, 10),
      slotStart: '',
    };
  }

  protected readonly customers = computed(() =>
    this.store.customers().filter((c) => c.active).sort((a, b) => a.name.localeCompare(b.name)),
  );

  protected readonly ownedVehicles = computed(() => {
    const owner = this.bookingDraft().customerId;
    return owner ? this.store.vehicles().filter((v) => v.customerId === owner && v.active) : [];
  });

  private closeBands(): void {
    this.editingSettings.set(false);
    this.addingClosure.set(false);
    this.removingClosure.set(null);
    this.booking.set(false);
    this.rescheduling.set(null);
    this.cancelling.set(null);
    this.bandError.set(null);
  }

  protected close(): void {
    this.closeBands();
  }

  /* --- hours ------------------------------------------------------------ */

  protected openSettings(): void {
    const s = this.settings();
    this.closeBands();
    this.settingsDraft.set({
      businessStartTime: s?.openFrom ?? '08:00',
      businessEndTime: s?.openTo ?? '18:00',
      dropoffSlotCapacity: s?.dropoffCapacityPerSlot ?? 3,
      pickupSlotCapacity: s?.pickupCapacityPerSlot ?? 3,
    });
    this.editingSettings.set(true);
  }

  protected async saveSettings(): Promise<void> {
    if (this.stf().invalid()) return;
    this.busy.set(true);
    this.bandError.set(null);
    const d = this.settingsDraft();
    const result = await this.store.updateSchedulingSettings({
      businessStartTime: d.businessStartTime,
      businessEndTime: d.businessEndTime,
      dropoffSlotCapacity: d.dropoffSlotCapacity ?? undefined,
      pickupSlotCapacity: d.pickupSlotCapacity ?? undefined,
    });
    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'The hours could not be saved.');
      return;
    }
    this.closeBands();
  }

  /* --- closures --------------------------------------------------------- */

  protected openClosure(): void {
    this.closeBands();
    this.closureDraft.set({ date: '', message: '' });
    this.addingClosure.set(true);
  }

  protected async saveClosure(): Promise<void> {
    if (this.clf().invalid()) return;
    this.busy.set(true);
    this.bandError.set(null);
    const d = this.closureDraft();
    const result = await this.store.createClosure({
      date: d.date,
      message: d.message.trim() || null,
    });
    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That date could not be closed.');
      return;
    }
    this.closeBands();
  }

  protected async removeClosure(c: Closure): Promise<void> {
    this.busy.set(true);
    const result = await this.store.deleteClosure(c.date);
    this.busy.set(false);
    this.removingClosure.set(null);
    if (!result.ok) this.bandError.set(result.error ?? null);
  }

  /** How many booked appointments a proposed closure would cancel. */
  protected bookedOn(date: string): number {
    return this.store
      .appointments()
      .filter((a) => a.status === 'SCHEDULED' && a.slotStart.slice(0, 10) === date).length;
  }

  /* --- booking ---------------------------------------------------------- */

  protected openBooking(): void {
    this.closeBands();
    this.bookingDraft.set(this.emptyBooking());
    this.booking.set(true);
    void this.loadSlots();
  }

  protected setGuest(forGuest: boolean): void {
    this.bookingDraft.update((d) => ({ ...d, forGuest, customerId: '', vehicleId: '' }));
  }

  protected onOwnerChange(): void {
    this.bookingDraft.update((d) => ({ ...d, vehicleId: '' }));
  }

  /** The open slots are a fact about the date, so they are re-read when it changes. */
  protected async loadSlots(): Promise<void> {
    const date = this.bookingDraft().date;
    if (!date) return;
    this.loadingSlots.set(true);
    this.slots.set(await this.store.availability('DROPOFF', date));
    this.loadingSlots.set(false);
    this.bookingDraft.update((d) => ({ ...d, slotStart: '' }));
  }

  protected async onDateChange(): Promise<void> {
    await this.loadSlots();
  }

  protected async book(): Promise<void> {
    if (this.bf().invalid()) return;
    this.busy.set(true);
    this.bandError.set(null);
    const d = this.bookingDraft();
    const result = await this.store.bookDropoff(
      d.forGuest
        ? {
            guestName: d.guestName.trim(),
            guestPhone: d.guestPhone.trim(),
            guestEmail: d.guestEmail.trim() || null,
            guestVehicleMake: d.guestVehicleMake.trim() || null,
            guestVehicleModel: d.guestVehicleModel.trim() || null,
            guestVehicleYear: d.guestVehicleYear,
            complaint: d.complaint.trim(),
            slotStart: d.slotStart,
          }
        : {
            customerId: d.customerId,
            vehicleId: d.vehicleId,
            complaint: d.complaint.trim(),
            slotStart: d.slotStart,
          },
    );
    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That booking was refused.');
      return;
    }
    this.closeBands();
  }

  /* --- reschedule and cancel -------------------------------------------- */

  protected async openReschedule(a: Appointment): Promise<void> {
    this.closeBands();
    this.rescheduleTo.set('');
    this.rescheduling.set(a.id);
    this.loadingSlots.set(true);
    this.slots.set(await this.store.availability(a.type, a.slotStart.slice(0, 10)));
    this.loadingSlots.set(false);
  }

  protected async reloadSlotsFor(a: Appointment, date: string): Promise<void> {
    this.loadingSlots.set(true);
    this.slots.set(await this.store.availability(a.type, date));
    this.loadingSlots.set(false);
    this.rescheduleTo.set('');
  }

  protected onRescheduleSlot(event: Event): void {
    this.rescheduleTo.set((event.target as HTMLSelectElement).value);
  }

  protected async confirmReschedule(a: Appointment): Promise<void> {
    const to = this.rescheduleTo();
    if (!to) return;
    this.busy.set(true);
    this.bandError.set(null);
    const result = await this.store.rescheduleAppointment(a.id, to);
    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That slot could not be taken.');
      return;
    }
    this.closeBands();
  }

  protected openCancel(a: Appointment): void {
    this.closeBands();
    this.cancelMessage.set('');
    this.cancelling.set(a.id);
  }

  protected onCancelMessage(event: Event): void {
    this.cancelMessage.set((event.target as HTMLInputElement).value);
  }

  protected async confirmCancel(a: Appointment): Promise<void> {
    this.busy.set(true);
    const result = await this.store.cancelAppointment(a.id, this.cancelMessage().trim() || null);
    this.busy.set(false);
    this.cancelling.set(null);
    if (!result.ok) this.bandError.set(result.error ?? null);
  }

  /** `2026-08-26T13:00:00Z` → `13:00`, in the operator's own zone. */
  protected slotTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  }
}
