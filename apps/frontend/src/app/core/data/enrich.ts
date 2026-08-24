import { Injectable, inject } from '@angular/core';
import { ApiError } from '../api/api-client';
import { ShopApi } from '../api/shop-api';
import type { Appointment, Customer, Vehicle, Worker, WorkOrder } from '../domain/models';
import { fullName, toCustomer, toVehicle, toWorker, vehicleLabel } from './mappers';

/**
 * The names behind the ids.
 *
 * `WorkOrderInfo` gives the board `customerId`, `vehicleId` and
 * `assignedMechanicId`; the board prints a person, a car and a plate. Closing
 * that gap is this file's whole job, and it does it in two passes:
 *
 *  1. Bulk. `/customers` and `/vehicles` return the shop in one page each, so
 *     for a shop-sized dataset the ids are almost always already in hand and no
 *     per-row request happens at all.
 *  2. Per id, for whatever the bulk pass missed — a customer deactivated out of
 *     the default listing, a mechanic (who is a Worker, and `/workers` is
 *     MANAGER-only). Each distinct id is fetched once, in parallel, and cached
 *     for the session. This is the agreed N+1: bounded by distinct ids on screen,
 *     not by rows, and never repeated for an id already seen.
 *
 * Enrichment is best-effort by design. A role the backend will not serve gets a
 * refusal recorded as a miss, and the UI falls back to the id-only rendering it
 * is written for. A missing name is never invented, and never blocks the board.
 */
@Injectable({ providedIn: 'root' })
export class Directory {
  private readonly api = inject(ShopApi);

  private readonly customers = new Map<string, Customer>();
  private readonly vehicles = new Map<string, Vehicle>();
  private readonly workers = new Map<string, Worker>();

  /** Ids already tried and refused or absent — never asked for a second time. */
  private readonly misses = new Set<string>();

  /** In-flight lookups, so N rows wanting one id produce one request. */
  private readonly pending = new Map<string, Promise<unknown>>();

  customer(id: string): Customer | undefined {
    return this.customers.get(id);
  }

  vehicle(id: string): Vehicle | undefined {
    return this.vehicles.get(id);
  }

  worker(id: string): Worker | undefined {
    return this.workers.get(id);
  }

  knownCustomers(): Customer[] {
    return [...this.customers.values()];
  }

  knownVehicles(): Vehicle[] {
    return [...this.vehicles.values()];
  }

  knownWorkers(): Worker[] {
    return [...this.workers.values()];
  }

  /** Seed the cache from a list the store already fetched. Pass 1. */
  primeCustomers(list: readonly Customer[]): void {
    for (const c of list) this.customers.set(c.id, c);
  }

  primeVehicles(list: readonly Vehicle[]): void {
    for (const v of list) this.vehicles.set(v.id, v);
  }

  primeWorkers(list: readonly Worker[]): void {
    for (const w of list) this.workers.set(w.id, w);
  }

  /**
   * Fetch every id these orders reference that is not already known. Pass 2.
   *
   * Resolves when the cache is as complete as this principal's permissions
   * allow; it never rejects, because a board that cannot name a customer is
   * still a board worth showing.
   */
  async resolveFor(orders: readonly WorkOrder[]): Promise<void> {
    const customerIds = new Set<string>();
    const vehicleIds = new Set<string>();
    const workerIds = new Set<string>();

    for (const order of orders) {
      if (!this.customers.has(order.customerId)) customerIds.add(order.customerId);
      if (!this.vehicles.has(order.vehicleId)) vehicleIds.add(order.vehicleId);
      const mechanic = order.assignedMechanicId;
      if (mechanic && !this.workers.has(mechanic)) workerIds.add(mechanic);
    }

    await Promise.all([
      ...[...customerIds].map((id) =>
        this.once(`customer:${id}`, async () => {
          this.customers.set(id, toCustomer(await this.api.customer(id)));
        }),
      ),
      ...[...vehicleIds].map((id) =>
        this.once(`vehicle:${id}`, async () => {
          this.vehicles.set(id, toVehicle(await this.api.vehicle(id)));
        }),
      ),
      // A mechanic is resolved through `/users/{id}`, which any staff principal
      // may call, rather than `/workers/{id}`, which only a MANAGER may.
      ...[...workerIds].map((id) =>
        this.once(`worker:${id}`, async () => {
          const user = await this.api.user(id);
          this.workers.set(id, toWorker(user));
        }),
      ),
    ]);
  }

  /** The ids an appointment references that are not already known. */
  async resolveForAppointments(list: readonly Appointment[]): Promise<void> {
    const customerIds = new Set<string>();
    const vehicleIds = new Set<string>();
    for (const a of list) {
      if (a.customerId && !this.customers.has(a.customerId)) customerIds.add(a.customerId);
      if (a.vehicleId && !this.vehicles.has(a.vehicleId)) vehicleIds.add(a.vehicleId);
    }
    await Promise.all([
      ...[...customerIds].map((id) =>
        this.once(`customer:${id}`, async () => {
          this.customers.set(id, toCustomer(await this.api.customer(id)));
        }),
      ),
      ...[...vehicleIds].map((id) =>
        this.once(`vehicle:${id}`, async () => {
          this.vehicles.set(id, toVehicle(await this.api.vehicle(id)));
        }),
      ),
    ]);
  }

  /**
   * A guest booking carries its own details inline and needs no lookup; a
   * customer booking carries ids and does.
   */
  decorateAppointment(a: Appointment): Appointment {
    const customer = a.customerId ? this.customers.get(a.customerId) : undefined;
    const vehicle = a.vehicleId ? this.vehicles.get(a.vehicleId) : undefined;
    return {
      ...a,
      customerName: customer?.name,
      vehicleLabel: vehicle ? vehicleLabel(vehicle) : undefined,
      vehiclePlate: vehicle?.licensePlate,
    };
  }

  /** Attach the labels the screens print. Pure — returns new objects. */
  decorate(order: WorkOrder): WorkOrder {
    const customer = this.customers.get(order.customerId);
    const vehicle = this.vehicles.get(order.vehicleId);
    const mechanic = order.assignedMechanicId ? this.workers.get(order.assignedMechanicId) : undefined;

    return {
      ...order,
      customerName: customer?.name,
      vehicleLabel: vehicle ? vehicleLabel(vehicle) : undefined,
      vehiclePlate: vehicle?.licensePlate,
      assignedMechanicName: mechanic?.name ?? null,
    };
  }

  /** Forget everything. Called on sign-out — the next operator is not this one. */
  clear(): void {
    this.customers.clear();
    this.vehicles.clear();
    this.workers.clear();
    this.misses.clear();
    this.pending.clear();
  }

  /**
   * Run `work` at most once per key for the lifetime of the cache, collapsing
   * concurrent callers onto one request and remembering refusals so a 403 does
   * not become one failed request per row, per render.
   */
  private once(key: string, work: () => Promise<void>): Promise<unknown> {
    if (this.misses.has(key)) return Promise.resolve();
    const existing = this.pending.get(key);
    if (existing) return existing;

    const run = work()
      .catch((error: unknown) => {
        // 403 and 404 are answers, not faults: this principal may not read that
        // record, or it is gone. Either way, stop asking.
        if (error instanceof ApiError && (error.isForbidden || error.isNotFound)) {
          this.misses.add(key);
          return;
        }
        // Anything else may be transient, so it is left un-cached and retryable.
        this.pending.delete(key);
      })
      .finally(() => {
        if (this.misses.has(key)) this.pending.delete(key);
      });

    this.pending.set(key, run);
    return run;
  }
}

/** Re-exported so callers need only one import for the common case. */
export { fullName, vehicleLabel };
