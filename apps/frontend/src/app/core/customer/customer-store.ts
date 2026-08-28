import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiError } from '../api/api-client';
import { ShopApi, type CustomerDropoffCommand, type VehicleCommand } from '../api/shop-api';
import type { AppointmentInfoDto, CustomerWorkOrderViewDto } from '../api/dto';
import { toAppointment, toBudget, toVehicle } from '../data/mappers';
import type { Appointment, Budget, Vehicle } from '../domain/models';
import type { WorkOrderStatus } from '../domain/enums';
import { isTerminal } from '../domain/lifecycle';
import { Session } from '../auth/session';

/**
 * One job as the customer is allowed to see it: the code, where it stands in
 * the procedure, and the budget they may be being asked to decide on. Nothing
 * else — the API withholds the mechanic and the diagnosis on purpose and this
 * console does not reconstruct them.
 */
export interface CustomerJob {
  readonly id: string;
  readonly orderCode: string;
  readonly status: WorkOrderStatus;
  readonly budget: Budget | null;
}

/** A job id the console could not read back. Kept, so the row can say why. */
export interface UnreadableJob {
  readonly id: string;
  readonly reason: string;
}

/**
 * The customer's own world.
 *
 * Deliberately *not* `ShopStore`. Every read in that store is a staff endpoint
 * that a CUSTOMER token is refused, so the two never share a cache and the
 * session drops the shop entirely when it leaves the Worker facet.
 *
 * ## The one place this console keeps its own record
 *
 * The API gives a customer no list of their own work orders and no list of
 * their own appointments — `GET /work-orders` and `GET /appointments` are both
 * staff-only, and the customer surface is addressed strictly by id
 * (`/work-orders/{id}/customer-view`, `/appointments/{id}/customer-cancel`).
 * Both gaps are filed in `docs/backend-requirements.md`.
 *
 * Until they land, this store keeps a local index in `localStorage`, scoped to
 * the signed-in user, of the jobs and bookings this browser has been shown. It
 * is a record of what this device has seen, never a claim to be the whole
 * account, and the screens that read it say so in as many words. Job ids are
 * re-read from the API on every load — the local half is only the address, and
 * the shop is always the authority on what is at it. Bookings are stored
 * whole, because there is no customer-facing endpoint to read one back.
 */
@Injectable({ providedIn: 'root' })
export class CustomerStore {
  private readonly api = inject(ShopApi);
  private readonly session = inject(Session);

  private readonly _vehicles = signal<Vehicle[]>([]);
  private readonly _jobs = signal<CustomerJob[]>([]);
  private readonly _unreadable = signal<UnreadableJob[]>([]);
  private readonly _bookings = signal<Appointment[]>([]);
  private readonly _loading = signal(false);
  private readonly _loaded = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly vehicles = this._vehicles.asReadonly();
  readonly jobs = this._jobs.asReadonly();
  readonly unreadable = this._unreadable.asReadonly();
  readonly bookings = this._bookings.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly loaded = this._loaded.asReadonly();
  readonly error = this._error.asReadonly();

  readonly activeVehicles = computed(() => this._vehicles().filter((v) => v.active));

  /** Jobs still moving. A delivered, refused or cancelled job is spent and reads quiet. */
  readonly openJobs = computed(() => this._jobs().filter((j) => !isTerminal(j.status)));

  /** The whole reason this console exists: a frozen budget waiting on a decision. */
  readonly awaitingDecision = computed(() =>
    this._jobs().filter((j) => j.status === 'WAITING_APPROVAL' && j.budget !== null),
  );

  readonly readyForPickup = computed(() =>
    this._jobs().filter((j) => j.status === 'WAITING_PICKUP'),
  );

  readonly upcomingBookings = computed(() => {
    const now = Date.now();
    return this._bookings()
      .filter((b) => b.status === 'SCHEDULED' && Date.parse(b.slotStart) > now)
      .sort((a, b) => a.slotStart.localeCompare(b.slotStart));
  });

  vehicle(id: string): Vehicle | undefined {
    return this._vehicles().find((v) => v.id === id);
  }

  job(id: string): CustomerJob | undefined {
    return this._jobs().find((j) => j.id === id);
  }

  /* --- loading ----------------------------------------------------------- */

  private inFlight: Promise<void> | null = null;

  load(force = false): Promise<void> {
    if (!force && this._loaded()) return Promise.resolve();
    if (this.inFlight) return this.inFlight;
    this.inFlight = this.runLoad().finally(() => {
      this.inFlight = null;
    });
    return this.inFlight;
  }

  private async runLoad(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      // `GET /vehicles` scopes itself to the caller when the caller is a
      // customer, so there is no filter to pass and nothing to leak by
      // forgetting one.
      const page = await this.api.vehicles();
      this._vehicles.set(page.content.map(toVehicle));

      this._bookings.set(this.readIndex().bookings.map(toAppointment));
      await this.readJobs();
      this._loaded.set(true);
    } catch (error) {
      this._error.set(
        error instanceof ApiError ? error.message : 'Could not load your account.',
      );
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Re-read every remembered job id.
   *
   * A 404 here is the API's answer for both "gone" and "not yours", by design —
   * it refuses to distinguish them, so neither does this. The id is kept and
   * the row says the shop no longer answers for it, rather than vanishing and
   * leaving the customer wondering what they mislaid.
   */
  private async readJobs(): Promise<void> {
    const ids = this.readIndex().jobs;
    if (ids.length === 0) {
      this._jobs.set([]);
      this._unreadable.set([]);
      return;
    }

    const results = await Promise.all(
      ids.map(async (id): Promise<CustomerJob | UnreadableJob> => {
        try {
          return toCustomerJob(await this.api.customerWorkOrder(id));
        } catch (error) {
          return {
            id,
            reason:
              error instanceof ApiError && error.isNotFound
                ? 'The shop no longer answers for this reference on your account.'
                : error instanceof ApiError
                  ? error.message
                  : 'Could not be read.',
          };
        }
      }),
    );

    this._jobs.set(results.filter(isJob));
    this._unreadable.set(results.filter((r): r is UnreadableJob => !isJob(r)));
  }

  /**
   * Take up a job reference — from the shop's email, or typed in by hand.
   *
   * Reads it before remembering it, so a mistyped reference fails in front of
   * the person who typed it rather than becoming a permanent broken row.
   */
  async trackJob(id: string): Promise<CustomerJob | null> {
    const trimmed = id.trim();
    if (!trimmed) return null;
    const job = toCustomerJob(await this.api.customerWorkOrder(trimmed));
    this.mutateIndex((index) => {
      if (!index.jobs.includes(trimmed)) index.jobs = [trimmed, ...index.jobs];
    });
    this._jobs.update((jobs) => [job, ...jobs.filter((j) => j.id !== job.id)]);
    this._unreadable.update((rows) => rows.filter((r) => r.id !== trimmed));
    return job;
  }

  /** Re-read one job after a decision, so the rest of the console agrees with it. */
  async refreshJob(id: string): Promise<void> {
    const job = toCustomerJob(await this.api.customerWorkOrder(id));
    this._jobs.update((jobs) =>
      jobs.some((j) => j.id === id) ? jobs.map((j) => (j.id === id ? job : j)) : [job, ...jobs],
    );
  }

  /** Drop a reference from this device's record. The job itself is untouched. */
  forgetJob(id: string): void {
    this.mutateIndex((index) => {
      index.jobs = index.jobs.filter((j) => j !== id);
    });
    this._jobs.update((jobs) => jobs.filter((j) => j.id !== id));
    this._unreadable.update((rows) => rows.filter((r) => r.id !== id));
  }

  /* --- the decision ------------------------------------------------------ */

  async approveBudget(jobId: string, budgetId: string): Promise<void> {
    await this.api.approveBudget(jobId, budgetId);
    await this.refreshJob(jobId);
  }

  async refuseBudget(jobId: string, budgetId: string, reason: string | null): Promise<void> {
    await this.api.refuseBudget(jobId, budgetId, reason?.trim() || null);
    await this.refreshJob(jobId);
  }

  /* --- vehicles ---------------------------------------------------------- */

  async addVehicle(command: VehicleCommand): Promise<Vehicle> {
    // A customer never names the owner: the API takes it from the token, and
    // sending one would be refused as an attempt to file against someone else.
    const { customerId: _ignored, ...own } = command;
    const vehicle = toVehicle(await this.api.createVehicle(own as VehicleCommand));
    this._vehicles.update((all) => [...all, vehicle]);
    return vehicle;
  }

  async updateVehicle(id: string, command: VehicleCommand): Promise<Vehicle> {
    const { customerId: _ignored, ...own } = command;
    const vehicle = toVehicle(await this.api.updateVehicle(id, own as VehicleCommand));
    this._vehicles.update((all) => all.map((v) => (v.id === id ? vehicle : v)));
    return vehicle;
  }

  /** Deactivates. The vehicle and its history stay on the shop's record. */
  async removeVehicle(id: string): Promise<void> {
    await this.api.deactivateVehicle(id);
    this._vehicles.update((all) =>
      all.map((v) => (v.id === id ? { ...v, active: false } : v)),
    );
  }

  /* --- bookings ---------------------------------------------------------- */

  slots(type: 'DROPOFF' | 'PICKUP', date: string): Promise<string[]> {
    return this.api.availability(type, date);
  }

  async bookDropoff(command: CustomerDropoffCommand): Promise<Appointment> {
    return this.remember(await this.api.bookCustomerDropoff(command));
  }

  async bookPickup(jobId: string, vehicleId: string, slotStart: string): Promise<Appointment> {
    return this.remember(await this.api.bookPickup(jobId, vehicleId, slotStart));
  }

  async cancelBooking(id: string, message: string | null): Promise<Appointment> {
    return this.remember(await this.api.cancelOwnAppointment(id, message?.trim() || null));
  }

  async rescheduleBooking(id: string, slotStart: string): Promise<Appointment> {
    const moved = await this.api.rescheduleOwnAppointment(id, slotStart);
    // A reschedule mints a new appointment and marks the old one RESCHEDULED,
    // so both are kept: the record should show the move, not hide it.
    return this.remember(moved);
  }

  private remember(dto: AppointmentInfoDto): Appointment {
    this.mutateIndex((index) => {
      index.bookings = [dto, ...index.bookings.filter((b) => b.id !== dto.id)].slice(0, 40);
    });
    const appointment = toAppointment(dto);
    this._bookings.update((all) => [appointment, ...all.filter((b) => b.id !== dto.id)]);
    return appointment;
  }

  reset(): void {
    this._vehicles.set([]);
    this._jobs.set([]);
    this._unreadable.set([]);
    this._bookings.set([]);
    this._loaded.set(false);
    this._error.set(null);
  }

  clearError(): void {
    this._error.set(null);
  }

  /* --- the local index --------------------------------------------------- */

  private key(): string | null {
    const id = this.session.customerId();
    return id ? `ars.mine.${id}` : null;
  }

  private readIndex(): LocalIndex {
    const key = this.key();
    if (!key) return { jobs: [], bookings: [] };
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return { jobs: [], bookings: [] };
      const parsed: unknown = JSON.parse(raw);
      if (typeof parsed !== 'object' || parsed === null) return { jobs: [], bookings: [] };
      const record = parsed as Partial<LocalIndex>;
      return {
        jobs: Array.isArray(record.jobs) ? record.jobs.filter(isNonEmptyString) : [],
        bookings: Array.isArray(record.bookings) ? record.bookings : [],
      };
    } catch {
      // Blocked site data, or a record written by an older shape. Either way
      // the console works — it just has nothing remembered to start from.
      return { jobs: [], bookings: [] };
    }
  }

  private mutateIndex(mutate: (index: LocalIndex) => void): void {
    const key = this.key();
    if (!key) return;
    const index = this.readIndex();
    mutate(index);
    try {
      localStorage.setItem(key, JSON.stringify(index));
    } catch {
      // The change still holds for this page view.
    }
  }
}

interface LocalIndex {
  jobs: string[];
  bookings: AppointmentInfoDto[];
}

function toCustomerJob(dto: CustomerWorkOrderViewDto): CustomerJob {
  return {
    id: dto.id,
    orderCode: dto.orderCode,
    status: dto.status,
    budget: dto.budget ? toBudget(dto.budget) : null,
  };
}

function isJob(value: CustomerJob | UnreadableJob): value is CustomerJob {
  return 'status' in value;
}

function isNonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && value.length > 0;
}
