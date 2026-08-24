import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiError } from '../api/api-client';
import {
  ShopApi,
  type ClosureCommand,
  type CustomerCommand,
  type CustomerProfileCommand,
  type DropoffOnBehalfCommand,
  type PartCommand,
  type PlaceOrderCommand,
  type ReceiveOrderCommand,
  type RepairServiceCommand,
  type SchedulingSettingsCommand,
  type StockPolicyCommand,
  type VehicleCommand,
  type VendorCommand,
  type WorkOrderFilter,
  type WorkerCommand,
} from '../api/shop-api';
import type { BudgetInfoDto } from '../api/dto';
import type { WorkOrderStatus } from '../domain/enums';
import { LIFECYCLE, nextStep, stepIndex } from '../domain/lifecycle';
import type {
  Appointment,
  Budget,
  BudgetLine,
  Closure,
  Customer,
  HistoryEntry,
  Part,
  PurchaseOrder,
  RepairService,
  ReorderRule,
  SchedulingSettings,
  StockMovement,
  Vehicle,
  Vendor,
  Worker,
  WorkOrder,
  WorkOrderBlock,
} from '../domain/models';
import { Directory } from './enrich';
import { toStatusCounts } from './mappers';
import {
  toAppointment,
  toBudget,
  toClosure,
  toCustomer,
  toHistoryEntry,
  toPart,
  toPurchaseOrder,
  toReorderRule,
  toRepairService,
  toSchedulingSettings,
  toShortfall,
  toStockMovement,
  toVehicle,
  toVendor,
  toWorkOrder,
  toWorker,
} from './mappers';
import {
  DEMO_APPOINTMENTS,
  DEMO_BLOCKS,
  DEMO_BUDGETS,
  DEMO_CLOSURES,
  DEMO_CUSTOMERS,
  DEMO_HISTORY,
  DEMO_NOW,
  DEMO_PARTS,
  DEMO_PURCHASE_ORDERS,
  DEMO_REORDER_RULES,
  DEMO_SERVICES,
  DEMO_SETTINGS,
  DEMO_STOCK_MOVEMENTS,
  DEMO_VEHICLES,
  DEMO_VENDORS,
  DEMO_WORKERS,
  DEMO_WORK_ORDERS,
  fallbackHistory,
} from './demo-data';

/** The outcome of a step, in the shop's terms rather than HTTP's. */
export interface StepResult {
  ok: boolean;
  error?: string;
}

export type StoreMode = 'live' | 'demo';

/**
 * The shop's state, in one place.
 *
 * Two sources sit behind one surface. In `live` mode every read is an HTTP call
 * whose result is cached in the signals below, and every mutation is the real
 * endpoint — the backend applies the rules and this store only reflects what it
 * answered. In `demo` mode the same signals are seeded from `demo-data.ts` and
 * the mutations re-apply those rules in memory.
 *
 * Demo mode is not decoration. The console outlives any particular database,
 * and the one behind it today holds a single bootstrap account and no work
 * orders at all, so `demo` is how the built screens can still be walked through.
 * The mode is explicit and visible in the masthead: nothing about this store
 * ever silently substitutes invented data for a failed request.
 *
 * The accessors are deliberately synchronous. Components read them inside
 * `computed()`, so loading is a separate, explicit step (`load()`), and what the
 * accessors return is whatever has arrived so far.
 */
@Injectable({ providedIn: 'root' })
export class ShopStore {
  private readonly api = inject(ShopApi);
  private readonly directory = inject(Directory);

  private readonly _mode = signal<StoreMode>('live');
  readonly mode = this._mode.asReadonly();
  readonly isDemo = computed(() => this._mode() === 'demo');

  /** `now` is frozen in demo mode so the seeded ages read as authored. */
  get now(): Date {
    return this._mode() === 'demo' ? DEMO_NOW : new Date();
  }

  private readonly _loading = signal(false);
  private readonly _loaded = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly loading = this._loading.asReadonly();
  readonly loaded = this._loaded.asReadonly();
  readonly error = this._error.asReadonly();

  /* --- the cache -------------------------------------------------------- */

  private readonly _workOrders = signal<WorkOrder[]>([]);
  private readonly _budgets = signal<Budget[]>([]);
  private readonly _parts = signal<Part[]>([]);
  private readonly _blocks = signal<WorkOrderBlock[]>([]);
  /**
   * Counts straight from the database. Null until the endpoint has answered, or
   * when it refuses — the board then falls back to counting what it holds and
   * says so, rather than printing zeros as though the shop were empty.
   */
  private readonly _summary = signal<Map<WorkOrderStatus, number> | null>(null);
  /** The filter the board is currently showing the results of. */
  private readonly _filter = signal<WorkOrderFilter>({});
  private readonly _appointments = signal<Appointment[]>([]);
  private readonly _movements = signal<StockMovement[]>([]);
  private readonly _history = signal<Record<string, HistoryEntry[]>>({});
  private readonly _services = signal<RepairService[]>([]);
  private readonly _vendors = signal<Vendor[]>([]);
  private readonly _purchaseOrders = signal<PurchaseOrder[]>([]);
  private readonly _reorderRules = signal<ReorderRule[]>([]);
  private readonly _customers = signal<Customer[]>([]);
  private readonly _vehicles = signal<Vehicle[]>([]);
  private readonly _workers = signal<Worker[]>([]);
  private readonly _closures = signal<Closure[]>([]);
  private readonly _settings = signal<SchedulingSettings | null>(null);

  readonly workOrders = this._workOrders.asReadonly();
  readonly budgets = this._budgets.asReadonly();
  readonly parts = this._parts.asReadonly();
  readonly appointments = this._appointments.asReadonly();
  readonly movements = this._movements.asReadonly();
  readonly services = this._services.asReadonly();
  readonly vendors = this._vendors.asReadonly();
  readonly purchaseOrders = this._purchaseOrders.asReadonly();
  readonly reorderRules = this._reorderRules.asReadonly();
  readonly customers = this._customers.asReadonly();
  readonly vehicles = this._vehicles.asReadonly();
  readonly workers = this._workers.asReadonly();
  readonly closures = this._closures.asReadonly();
  readonly settings = this._settings.asReadonly();

  readonly lifecycle = LIFECYCLE;

  readonly filter = this._filter.asReadonly();
  /** True while the counts are the database's rather than this page's. */
  readonly countsAreExact = computed(() => this._summary() !== null);

  /* --- derived ---------------------------------------------------------- */

  /** Live jobs — everything that has not left the shop. */
  readonly liveWorkOrders = computed(() =>
    this._workOrders()
      .filter((o) => o.status !== 'DELIVERED' && o.status !== 'REFUSED')
      .sort((a, b) => stepIndex(a.status) - stepIndex(b.status) || a.orderCode.localeCompare(b.orderCode)),
  );

  readonly closedWorkOrders = computed(() =>
    this._workOrders().filter((o) => o.status === 'DELIVERED' || o.status === 'REFUSED'),
  );

  /**
   * Count per status, for the board's step filter.
   *
   * The database's own counts when they are available, because counting the
   * loaded page is only right while the whole shop fits in one — and it stops
   * being right silently. Falls back to the page otherwise.
   */
  readonly statusCounts = computed(() => {
    const exact = this._summary();
    if (exact) return exact;
    const counts = new Map<WorkOrderStatus, number>();
    for (const o of this._workOrders()) counts.set(o.status, (counts.get(o.status) ?? 0) + 1);
    return counts;
  });

  readonly blockedCount = computed(() => this._blocks().filter((b) => b.blocked).length);

  /** How many vehicles each customer has on record — counted, never fetched. */
  readonly vehicleCounts = computed(() => {
    const counts = new Map<string, number>();
    for (const v of this._vehicles()) counts.set(v.customerId, (counts.get(v.customerId) ?? 0) + 1);
    return counts;
  });

  /* --- point reads ------------------------------------------------------ */

  workOrder(id: string): WorkOrder | undefined {
    return this._workOrders().find((o) => o.id === id);
  }

  budget(id: string | null): Budget | undefined {
    return id ? this._budgets().find((b) => b.id === id) : undefined;
  }

  budgetFor(workOrderId: string): Budget | undefined {
    return this._budgets().find((b) => b.workOrderId === workOrderId);
  }

  part(id: string): Part | undefined {
    return this._parts().find((p) => p.id === id);
  }

  service(id: string): RepairService | undefined {
    return this._services().find((s) => s.id === id);
  }

  vendor(id: string): Vendor | undefined {
    return this._vendors().find((v) => v.id === id);
  }

  customer(id: string): Customer | undefined {
    return this._customers().find((c) => c.id === id) ?? this.directory.customer(id);
  }

  vehicle(id: string): Vehicle | undefined {
    return this._vehicles().find((v) => v.id === id) ?? this.directory.vehicle(id);
  }

  worker(id: string): Worker | undefined {
    return this._workers().find((w) => w.id === id) ?? this.directory.worker(id);
  }

  /**
   * Whether this order is blocked, and by what.
   *
   * The read endpoint the brief specified now exists, so a blocked row is a
   * fact rather than an inference. It is still only fetched for orders that
   * could plausibly be blocked (see `refreshBlocks`), and a role the endpoint
   * refuses — an ATTENDANT gets 403 — yields `blocked: false` with no
   * shortfalls, which renders as "nothing known", never as "all clear".
   */
  blockFor(workOrderId: string): WorkOrderBlock {
    return (
      this._blocks().find((b) => b.workOrderId === workOrderId) ?? {
        workOrderId,
        blocked: false,
        shortfalls: [],
      }
    );
  }

  history(workOrderId: string): HistoryEntry[] {
    const known = this._history()[workOrderId];
    if (known) return known;
    if (this._mode() === 'demo') {
      const order = this.workOrder(workOrderId);
      return order ? fallbackHistory(order) : [];
    }
    return [];
  }

  /* --- loading ---------------------------------------------------------- */

  setMode(mode: StoreMode): void {
    if (this._mode() === mode) return;
    this._mode.set(mode);
    this._loaded.set(false);
    this.directory.clear();
  }

  /**
   * Drop the whole shop.
   *
   * Called on sign-out and whenever the session leaves the Worker facet.
   * Everything in here is read from staff endpoints, so a customer principal
   * holding a loaded shop is holding data their own token could never fetch
   * again — and every one of those rows is somebody else's business.
   */
  reset(): void {
    this._loaded.set(false);
    this._loading.set(false);
    this._error.set(null);
    this._workOrders.set([]);
    this._budgets.set([]);
    this._parts.set([]);
    this._blocks.set([]);
    this._summary.set(null);
    this._filter.set({});
    this._appointments.set([]);
    this._movements.set([]);
    this._history.set({});
    this._services.set([]);
    this._vendors.set([]);
    this._purchaseOrders.set([]);
    this._reorderRules.set([]);
    this._customers.set([]);
    this._vehicles.set([]);
    this._workers.set([]);
    this._closures.set([]);
    this._settings.set(null);
    this.actingWorkerId = null;
    this.directory.clear();
  }

  /**
   * Fill the cache. Safe to call from every screen's constructor: it runs once
   * unless `force` is set, and concurrent callers share the one in-flight load.
   */
  private inFlight: Promise<void> | null = null;

  load(force = false): Promise<void> {
    if (!force && this._loaded()) return Promise.resolve();
    if (this.inFlight) return this.inFlight;

    this.inFlight = (this._mode() === 'demo' ? this.loadDemo() : this.loadLive()).finally(() => {
      this.inFlight = null;
    });
    return this.inFlight;
  }

  private async loadDemo(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);

    this._workOrders.set(structuredClone(DEMO_WORK_ORDERS));
    this._budgets.set(structuredClone(DEMO_BUDGETS));
    this._parts.set(structuredClone(DEMO_PARTS));
    this._blocks.set(structuredClone(DEMO_BLOCKS));
    this._appointments.set(structuredClone(DEMO_APPOINTMENTS));
    this._movements.set(structuredClone(DEMO_STOCK_MOVEMENTS));
    this._history.set(reverseAll(structuredClone(DEMO_HISTORY)));
    this._services.set(structuredClone(DEMO_SERVICES));
    this._vendors.set(structuredClone(DEMO_VENDORS));
    this._purchaseOrders.set(structuredClone(DEMO_PURCHASE_ORDERS));
    this._reorderRules.set(structuredClone(DEMO_REORDER_RULES));
    this._customers.set(structuredClone(DEMO_CUSTOMERS));
    this._vehicles.set(structuredClone(DEMO_VEHICLES));
    this._workers.set(structuredClone(DEMO_WORKERS));
    this._closures.set(structuredClone(DEMO_CLOSURES));
    this._settings.set(structuredClone(DEMO_SETTINGS));

    this._loading.set(false);
    this._loaded.set(true);
  }

  /**
   * Everything the console needs, in as few requests as the API allows.
   *
   * The sections are fetched together rather than per-route because they are
   * cross-referenced constantly — the board names a customer, the inventory
   * names a vendor — and a shop-sized dataset is one page each. Every section is
   * settled independently: a role that may not read `/workers` still gets a
   * fully working Work Orders board, with that one list simply empty.
   */
  private async loadLive(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);

    const orders = await this.safe(() => this.api.workOrders(this._filter()), 'work orders');
    void this.refreshSummary();

    const results = await Promise.all([
      this.safe(() => this.api.parts(), 'parts'),
      this.safe(() => this.api.partStock(), 'stock'),
      this.safe(() => this.api.services(), 'services'),
      this.safe(() => this.api.vendors(), 'vendors'),
      this.safe(() => this.api.purchaseOrders(), 'purchase orders'),
      this.safe(() => this.api.stockPolicies(), 'reorder rules'),
      this.safe(() => this.api.customers(), 'customers'),
      this.safe(() => this.api.vehicles(), 'vehicles'),
      this.safe(() => this.api.workers(), 'workers'),
      this.safe(() => this.api.appointments(), 'appointments'),
      this.safe(() => this.api.schedulingSettings(), 'scheduling settings'),
      this.safe(() => this.api.closures(), 'closures'),
    ]);

    const [
      parts,
      stock,
      services,
      vendors,
      purchaseOrders,
      policies,
      customers,
      vehicles,
      workers,
      appointments,
      settings,
      closures,
    ] = results;

    // The catalog and the derived ledger arrive separately and are joined here.
    const stockByPart = new Map((stock?.content ?? []).map((s) => [s.partId, s]));
    this._parts.set((parts?.content ?? []).map((p) => toPart(p, stockByPart.get(p.id))));

    this._services.set((services?.content ?? []).map(toRepairService));
    this._vendors.set((vendors?.content ?? []).map(toVendor));
    this._purchaseOrders.set((purchaseOrders?.content ?? []).map(toPurchaseOrder));
    this._reorderRules.set((policies?.content ?? []).map(toReorderRule));
    const bookings = (appointments?.content ?? []).map(toAppointment);
    this._settings.set(settings ? toSchedulingSettings(settings) : null);
    this._closures.set((closures ?? []).map(toClosure));

    const customerList = (customers?.content ?? []).map(toCustomer);
    const vehicleList = (vehicles?.content ?? []).map(toVehicle);
    // `/workers` lists every User with a Worker facet; MANAGER-only, so this is
    // routinely empty and the roster falls back to per-id resolution.
    const workerList = (workers?.content ?? []).filter((u) => u.worker).map(toWorker);

    this._customers.set(customerList);
    this._vehicles.set(vehicleList);
    this._workers.set(workerList);

    this.directory.primeCustomers(customerList);
    this.directory.primeVehicles(vehicleList);
    this.directory.primeWorkers(workerList);

    // Pass 2: the ids the bulk lists did not cover, then decorate.
    const bare = (orders?.content ?? []).map(toWorkOrder);
    await Promise.all([
      this.directory.resolveFor(bare),
      this.directory.resolveForAppointments(bookings),
    ]);
    this._workOrders.set(bare.map((o) => this.directory.decorate(o)));
    this._appointments.set(bookings.map((a) => this.directory.decorateAppointment(a)));

    await Promise.all([this.refreshBudgets(), this.refreshBlocks()]);

    this._loading.set(false);
    this._loaded.set(true);
  }

  /**
   * One request per work order that has a budget.
   *
   * There is no `/budgets?workOrderId=` and no bulk read, so this is the N+1 the
   * console runs knowingly. It is bounded by open work orders, runs in parallel,
   * and only ever fetches an id the order itself named.
   */
  private async refreshBudgets(): Promise<void> {
    const ids = this._workOrders()
      .map((o) => o.budgetId)
      .filter((id): id is string => id !== null);

    const budgets = await Promise.all(ids.map((id) => this.safe(() => this.api.budget(id), 'budget')));
    this._budgets.set(budgets.filter((b) => b !== null).map(toBudget));
  }

  /**
   * Shortfalls, for the orders where a shortfall would actually mean something.
   *
   * Only an APPROVED order is about to consume stock, so only those are asked
   * about — the board's blocked band is about the next step being refused, and
   * no other status has that step in front of it.
   */
  private async refreshBlocks(): Promise<void> {
    const candidates = this._workOrders().filter((o) => o.status === 'APPROVED');
    const available = new Map(this._parts().map((p) => [p.id, p.available]));

    const blocks = await Promise.all(
      candidates.map(async (order): Promise<WorkOrderBlock | null> => {
        const shortfalls = await this.safe(() => this.api.blockingShortfalls(order.id), 'shortfalls');
        if (shortfalls === null) return null;
        return {
          workOrderId: order.id,
          blocked: shortfalls.length > 0,
          shortfalls: shortfalls.map((s) => toShortfall(s, available.get(s.partId))),
        };
      }),
    );

    this._blocks.set(blocks.filter((b): b is WorkOrderBlock => b !== null));
  }

  /**
   * Re-run the board's query against the API.
   *
   * The filter is the server's problem, not a predicate over a page: this is
   * what keeps the board correct once the shop outgrows one request. Enrichment
   * and the dependent reads (budgets, shortfalls) follow the new result set.
   */
  async loadWorkOrders(filter: WorkOrderFilter): Promise<void> {
    this._filter.set(filter);
    if (this._mode() === 'demo') return;

    const page = await this.safe(() => this.api.workOrders(filter), 'work orders');
    if (!page) return;

    const bare = page.content.map(toWorkOrder);
    await this.directory.resolveFor(bare);
    this._workOrders.set(bare.map((o) => this.directory.decorate(o)));
    await Promise.all([this.refreshBudgets(), this.refreshBlocks()]);
  }

  /**
   * Make sure one work order is loaded, whatever the board is filtered to.
   *
   * The detail view is reachable by link, so it cannot assume the board's
   * current query happens to include the order being opened.
   */
  async ensureWorkOrder(id: string): Promise<void> {
    if (this._mode() === 'demo' || this.workOrder(id)) return;
    const dto = await this.safe(() => this.api.workOrder(id), 'work order');
    if (!dto) return;
    const order = toWorkOrder(dto);
    await this.directory.resolveFor([order]);
    this.applyWorkOrder(order);
    await Promise.all([this.refreshBudgets(), this.refreshBlocks()]);
  }

  /**
   * The database's own counts.
   *
   * Failure here is deliberately quiet. There is a correct fallback — counting
   * the rows in hand — and the board says which it is showing, so raising the
   * API fault band would be alarming about something the operator cannot act on
   * and that costs them nothing. Every other read has no fallback and does raise.
   */
  private async refreshSummary(): Promise<void> {
    if (this._mode() === 'demo') return;
    try {
      this._summary.set(toStatusCounts(await this.api.workOrderSummary()));
    } catch {
      this._summary.set(null);
    }
  }

  /** A work order's Timeline, fetched on demand by the detail view. */
  async loadHistory(workOrderId: string): Promise<void> {
    if (this._mode() === 'demo') return;
    const page = await this.safe(() => this.api.workOrderHistory(workOrderId), 'history');
    if (!page) return;
    this._history.update((all) => ({ ...all, [workOrderId]: page.content.map(toHistoryEntry) }));
  }

  /**
   * Run a read, and let it fail without taking the console with it.
   *
   * A refusal (403) is an expected answer for several of these lists — the role
   * matrix is real — so it is not surfaced as an error at all; the section is
   * simply empty, which is what the role is entitled to see. Anything else is
   * recorded once, so the shell can say the API is unhappy without every screen
   * having to.
   */
  private async safe<T>(work: () => Promise<T>, what: string): Promise<T | null> {
    try {
      return await work();
    } catch (error) {
      if (error instanceof ApiError && (error.isForbidden || error.isNotFound)) return null;
      const message =
        error instanceof ApiError ? error.message : `Could not load ${what}.`;
      this._error.set(message);
      return null;
    }
  }

  /* ---------------------------------------------------------------------
     Mutations
     --------------------------------------------------------------------- */

  /**
   * Perform the next lawful step of the procedure.
   *
   * In live mode this dispatches to the one endpoint that performs that step —
   * there is no generic "advance" on the backend, and there should not be — and
   * the returned `WorkOrderInfo` is the new truth. In demo mode the same rules
   * are applied in memory.
   */
  async advance(workOrderId: string): Promise<StepResult> {
    const order = this.workOrder(workOrderId);
    if (!order) return { ok: false, error: 'Work order not found.' };

    const step = nextStep(order.status);
    if (!step) return { ok: false, error: 'This work order has reached the end of the procedure.' };

    if (this._mode() === 'demo') return this.advanceDemo(order, step.status);

    // Finishing diagnostics is the one step that cannot be a bare button: the
    // backend requires a written diagnosis and at least one budget line, and
    // inventing either would put words in a mechanic's mouth. The console has no
    // form for it yet, so it says so rather than sending something empty.
    if (step.status === 'BUDGET_IN_DRAFT') {
      return {
        ok: false,
        error:
          'Finishing diagnostics needs a written diagnosis and at least one budget line. Record them with “Finish diagnostics” on the detail view — this console has no form for it yet.',
      };
    }

    try {
      const updated = await this.performStep(order, step.status);
      this.applyWorkOrder(updated);

      // A step that moves stock or freezes a budget changes more than the order.
      if (step.status === 'WAITING_APPROVAL' || step.status === 'IN_PROGRESS') {
        await Promise.all([this.refreshBudgets(), this.refreshParts()]);
      }
      await this.refreshBlocks();
      await Promise.all([this.refreshSummary(), this.loadHistory(workOrderId)]);
      return { ok: true };
    } catch (error) {
      return { ok: false, error: describe(error) };
    }
  }

  /** The endpoint that performs each step. Mirrors `lifecycle.ts` exactly. */
  private async performStep(order: WorkOrder, target: WorkOrderStatus): Promise<WorkOrder> {
    switch (target) {
      case 'WAITING_DIAGNOSTICS':
        return toWorkOrder(await this.api.requestDiagnostics(order.id));
      case 'IN_DIAGNOSTICS': {
        // The backend assigns the mechanic as part of starting, so the operator
        // performing the step takes the job unless one is already assigned.
        const mechanicId = order.assignedMechanicId ?? this.actingWorkerId;
        if (!mechanicId) {
          throw new ApiError(0, 'No mechanic to assign — sign in as the mechanic taking this job.', null, null);
        }
        return toWorkOrder(await this.api.startDiagnostics(order.id, mechanicId));
      }
      case 'IN_PROGRESS':
        return toWorkOrder(await this.api.startService(order.id));
      case 'FINISHED':
        return toWorkOrder(await this.api.finishService(order.id));
      case 'WAITING_PICKUP':
        return toWorkOrder(await this.api.pickupReady(order.id));
      case 'DELIVERED':
        return toWorkOrder(await this.api.recordDelivery(order.id));
      case 'WAITING_APPROVAL': {
        // Step 5 is performed against the Budget, not the Work Order.
        if (!order.budgetId) throw new ApiError(0, 'This work order has no budget to send.', null, null);
        await this.api.sendBudget(order.budgetId);
        return toWorkOrder(await this.api.workOrder(order.id));
      }
      default:
        throw new ApiError(0, 'That step is not one the shop performs.', null, null);
    }
  }

  /**
   * Who is performing the step. Set by `Session` at sign-in rather than injected,
   * because `Session` reads this store and the cycle is not worth the ceremony.
   */
  actingWorkerId: string | null = null;

  /**
   * Record the diagnosis and open the budget draft, seeded with its first lines.
   * The backend does both in one transaction; so does this.
   */
  async finishDiagnostics(
    workOrderId: string,
    diagnosis: string,
    lines: readonly { type: 'PART' | 'SERVICE'; quantity: number; partId?: string; serviceId?: string }[],
  ): Promise<StepResult> {
    if (!diagnosis.trim()) return { ok: false, error: 'A diagnosis is required.' };
    if (lines.length === 0) return { ok: false, error: 'A budget needs at least one line.' };

    try {
      const updated = await this.api.finishDiagnostics(workOrderId, diagnosis, lines);
      this.applyWorkOrder(toWorkOrder(updated));
      await Promise.all([this.refreshBudgets(), this.refreshParts()]);
      await this.loadHistory(workOrderId);
      return { ok: true };
    } catch (error) {
      return { ok: false, error: describe(error) };
    }
  }

  /** A budget stuck in WAITING_SEND may be resent as often as needed. */
  async resendBudget(budgetId: string): Promise<StepResult> {
    if (this._mode() === 'demo') {
      const stamp = new Date().toISOString();
      this._budgets.update((budgets) =>
        budgets.map((b) => (b.id === budgetId ? { ...b, status: 'SENT', sentAt: stamp } : b)),
      );
      return { ok: true };
    }
    try {
      this.applyBudget(toBudget(await this.api.resendBudget(budgetId)));
      return { ok: true };
    } catch (error) {
      return { ok: false, error: describe(error) };
    }
  }

  /**
   * Draft-only. Every add, removal and quantity change reserves or releases
   * stock immediately, so the parts cache is refreshed alongside the budget —
   * the consequence and the action belong in the same view.
   */
  async addLine(
    budgetId: string,
    line: { type: 'PART' | 'SERVICE'; quantity: number; partId?: string; serviceId?: string; description?: string; unitPrice?: number },
  ): Promise<StepResult> {
    if (this._mode() === 'demo') {
      this.addLineDemo(budgetId, line);
      return { ok: true };
    }
    return this.budgetEdit(() =>
      this.api.addBudgetLine(budgetId, {
        type: line.type,
        quantity: line.quantity,
        partId: line.partId,
        serviceId: line.serviceId,
      }),
    );
  }

  async removeLine(budgetId: string, lineId: string): Promise<StepResult> {
    if (this._mode() === 'demo') {
      this.removeLineDemo(budgetId, lineId);
      return { ok: true };
    }
    return this.budgetEdit(() => this.api.removeBudgetLine(budgetId, lineId));
  }

  async setLineQuantity(budgetId: string, lineId: string, quantity: number): Promise<StepResult> {
    if (quantity < 1) return { ok: false, error: 'A line needs at least one unit.' };
    if (this._mode() === 'demo') {
      this.setLineQuantityDemo(budgetId, lineId, quantity);
      return { ok: true };
    }
    return this.budgetEdit(() => this.api.changeBudgetLineQuantity(budgetId, lineId, quantity));
  }

  async startLine(budgetId: string, lineId: string): Promise<StepResult> {
    return this.lineTiming(budgetId, lineId, 'startedAt', (workOrderId) =>
      this.api.startLine(workOrderId, lineId),
    );
  }

  async finishLine(budgetId: string, lineId: string): Promise<StepResult> {
    return this.lineTiming(budgetId, lineId, 'finishedAt', (workOrderId) =>
      this.api.finishLine(workOrderId, lineId),
    );
  }

  /** Line timing is addressed by work order on the wire, by budget in the UI. */
  private async lineTiming(
    budgetId: string,
    lineId: string,
    field: 'startedAt' | 'finishedAt',
    call: (workOrderId: string) => Promise<unknown>,
  ): Promise<StepResult> {
    const budget = this.budget(budgetId);
    if (!budget) return { ok: false, error: 'That budget is not loaded.' };

    if (this._mode() === 'demo') {
      const stamp = new Date().toISOString();
      this.patchLineDemo(budgetId, lineId, { [field]: stamp });
      return { ok: true };
    }

    try {
      await call(budget.workOrderId);
      this.applyBudget(toBudget(await this.api.budget(budgetId)));
      return { ok: true };
    } catch (error) {
      return { ok: false, error: describe(error) };
    }
  }

  /** Every draft edit answers with the whole budget, and moves stock as it goes. */
  private async budgetEdit(call: () => Promise<BudgetInfoDto>): Promise<StepResult> {
    try {
      this.applyBudget(toBudget(await call()));
      await this.refreshParts();
      return { ok: true };
    } catch (error) {
      return { ok: false, error: describe(error) };
    }
  }

  /** Checking in a drop-off produces a work order; a pickup delivers one. */
  async checkIn(appointmentId: string): Promise<StepResult> {
    if (this._mode() === 'demo') {
      const stamp = new Date().toISOString();
      this._appointments.update((list) =>
        list.map((a) =>
          a.id === appointmentId ? { ...a, status: 'COMPLETED', checkedInAt: stamp, updatedAt: stamp } : a,
        ),
      );
      return { ok: true };
    }
    try {
      this.applyAppointment(this.directory.decorateAppointment(toAppointment(await this.api.checkIn(appointmentId))));
      // Checking in a drop-off opens a work order, so the board is now stale.
      await this.load(true);
      return { ok: true };
    } catch (error) {
      return { ok: false, error: describe(error) };
    }
  }

  async cancelAppointment(appointmentId: string, message: string | null): Promise<StepResult> {
    if (this._mode() === 'demo') {
      const stamp = new Date().toISOString();
      this._appointments.update((list) =>
        list.map((a) =>
          a.id === appointmentId
            ? { ...a, status: 'CANCELLED', cancelReason: 'STAFF_REQUESTED', cancelMessage: message, updatedAt: stamp }
            : a,
        ),
      );
      return { ok: true };
    }
    try {
      this.applyAppointment(
        this.directory.decorateAppointment(toAppointment(await this.api.cancelAppointment(appointmentId, message))),
      );
      return { ok: true };
    } catch (error) {
      return { ok: false, error: describe(error) };
    }
  }

  /* ---------------------------------------------------------------------
     Records — create, update, deactivate.

     Every one of these is the API's own call; nothing is applied optimistically.
     A record appears in the console only once the backend has said it exists,
     because a row that shows as saved and then vanishes is worse than a
     half-second wait. Each returns the shop's `StepResult` so the calling band
     can print the refusal against the form the operator is still looking at.
     --------------------------------------------------------------------- */

  async createCustomer(command: CustomerCommand): Promise<StepResult & { id?: string }> {
    return this.write(async () => {
      const created = await this.api.createCustomer(command);
      const customer = toCustomer(created);
      this._customers.update((all) => [...all, customer]);
      this.directory.primeCustomers([customer]);
      return created.id;
    });
  }

  async updateCustomer(id: string, command: CustomerProfileCommand): Promise<StepResult> {
    return this.write(async () => {
      const customer = toCustomer(await this.api.updateCustomer(id, command));
      this._customers.update((all) => all.map((c) => (c.id === id ? customer : c)));
      this.directory.primeCustomers([customer]);
      // The board prints the customer's name on every row of theirs.
      this.redecorate();
      return id;
    });
  }

  /**
   * Deactivation, not deletion. The API keeps the record and its history; the
   * console keeps the row and marks it, rather than making it disappear.
   */
  async setCustomerActive(id: string, active: boolean): Promise<StepResult> {
    return this.write(async () => {
      if (active) await this.api.reactivateCustomer(id);
      else await this.api.deactivateCustomer(id);
      const refreshed = toCustomer(await this.api.customer(id));
      this._customers.update((all) => all.map((c) => (c.id === id ? refreshed : c)));
      this.directory.primeCustomers([refreshed]);
      return id;
    });
  }

  async createVehicle(command: VehicleCommand): Promise<StepResult & { id?: string }> {
    return this.write(async () => {
      const vehicle = toVehicle(await this.api.createVehicle(command));
      this._vehicles.update((all) => [...all, vehicle]);
      this.directory.primeVehicles([vehicle]);
      return vehicle.id;
    });
  }

  async updateVehicle(id: string, command: VehicleCommand): Promise<StepResult> {
    return this.write(async () => {
      const vehicle = toVehicle(await this.api.updateVehicle(id, command));
      this._vehicles.update((all) => all.map((v) => (v.id === id ? vehicle : v)));
      this.directory.primeVehicles([vehicle]);
      this.redecorate();
      return id;
    });
  }

  async deactivateVehicle(id: string): Promise<StepResult> {
    return this.write(async () => {
      await this.api.deactivateVehicle(id);
      const vehicle = toVehicle(await this.api.vehicle(id));
      this._vehicles.update((all) => all.map((v) => (v.id === id ? vehicle : v)));
      this.directory.primeVehicles([vehicle]);
      return id;
    });
  }

  async createPart(command: PartCommand): Promise<StepResult> {
    return this.write(async () => {
      await this.api.createPart(command);
      await this.refreshParts();
      return undefined;
    });
  }

  async updatePart(id: string, command: PartCommand): Promise<StepResult> {
    return this.write(async () => {
      await this.api.updatePart(id, command);
      await this.refreshParts();
      return id;
    });
  }

  async deactivatePart(id: string): Promise<StepResult> {
    return this.write(async () => {
      await this.api.deactivatePart(id);
      await this.refreshParts();
      return id;
    });
  }

  /**
   * A signed correction to what is on the shelf — a physical count, breakage, or
   * stock found on the wrong rack. Zero is rejected by the API; a reason is
   * required, because an unexplained movement in an append-only ledger is a
   * fact nobody can audit later.
   */
  async adjustStock(partId: string, quantity: number, reason: string): Promise<StepResult> {
    if (quantity === 0) return { ok: false, error: 'An adjustment of zero changes nothing.' };
    if (!reason.trim()) return { ok: false, error: 'An adjustment needs a reason.' };
    return this.write(async () => {
      await this.api.adjustStock(partId, quantity, reason.trim());
      await this.refreshParts();
      // Consuming or replenishing stock can clear or create a blocked order.
      await this.refreshBlocks();
      return partId;
    });
  }

  async createService(command: RepairServiceCommand): Promise<StepResult> {
    return this.write(async () => {
      await this.api.createService(command);
      await this.refreshServices();
      return undefined;
    });
  }

  async updateService(id: string, command: RepairServiceCommand): Promise<StepResult> {
    return this.write(async () => {
      await this.api.updateService(id, command);
      await this.refreshServices();
      return id;
    });
  }

  async deactivateService(id: string): Promise<StepResult> {
    return this.write(async () => {
      await this.api.deactivateService(id);
      await this.refreshServices();
      return id;
    });
  }

  /** Open a work order against a customer's vehicle. ATTENDANT or MANAGER. */
  async createWorkOrder(
    customerId: string,
    vehicleId: string,
    complaint: string,
  ): Promise<StepResult & { id?: string }> {
    return this.write(async () => {
      const created = toWorkOrder(await this.api.createWorkOrder(customerId, vehicleId, complaint));
      await this.directory.resolveFor([created]);
      this.applyWorkOrder(created);
      await this.refreshSummary();
      return created.id;
    });
  }

  /**
   * Take the job, or assign it. The backend assigns the mechanic as part of
   * starting diagnostics, so the two are one call and one decision.
   */
  async startDiagnostics(workOrderId: string, mechanicId: string): Promise<StepResult> {
    return this.write(async () => {
      const updated = toWorkOrder(await this.api.startDiagnostics(workOrderId, mechanicId));
      await this.directory.resolveFor([updated]);
      this.applyWorkOrder(updated);
      await this.loadHistory(workOrderId);
      return workOrderId;
    });
  }

  private async refreshServices(): Promise<void> {
    const services = await this.safe(() => this.api.services(), 'services');
    if (services) this._services.set(services.content.map(toRepairService));
  }

  /** Re-attach enriched labels after a name or plate changed underneath them. */
  private redecorate(): void {
    this._workOrders.update((all) => all.map((o) => this.directory.decorate(o)));
  }

  /**
   * Run a write and translate its failure into the band's own language.
   * Demo mode refuses outright rather than pretending: the synthetic shop is a
   * reading surface, and a form that appeared to save into it would be a lie.
   */
  private async write(work: () => Promise<string | undefined>): Promise<StepResult & { id?: string }> {
    if (this._mode() === 'demo') {
      return { ok: false, error: 'Demo mode is read-only. Sign in to record anything.' };
    }
    try {
      const id = await work();
      return { ok: true, id };
    } catch (error) {
      return { ok: false, error: describe(error) };
    }
  }

  /* ---------------------------------------------------------------------
     Purchasing, thresholds and the roster.
     --------------------------------------------------------------------- */

  async createVendor(command: VendorCommand): Promise<StepResult> {
    return this.write(async () => {
      await this.api.createVendor(command);
      await this.refreshVendors();
      return undefined;
    });
  }

  async updateVendor(id: string, command: VendorCommand): Promise<StepResult> {
    return this.write(async () => {
      await this.api.updateVendor(id, command);
      await this.refreshVendors();
      return id;
    });
  }

  async deactivateVendor(id: string): Promise<StepResult> {
    return this.write(async () => {
      await this.api.deactivateVendor(id);
      await this.refreshVendors();
      return id;
    });
  }

  /**
   * Placing an order forwards it to the vendor. It raises nothing on the shelf —
   * stock moves only when the parts actually arrive and a receipt is recorded.
   */
  async placePurchaseOrder(command: PlaceOrderCommand): Promise<StepResult> {
    return this.write(async () => {
      await this.api.placePurchaseOrder(command);
      await this.refreshPurchaseOrders();
      return undefined;
    });
  }

  /** Receiving raises on-hand stock and moves the part's average cost. */
  async receivePurchaseOrder(id: string, command: ReceiveOrderCommand): Promise<StepResult> {
    return this.write(async () => {
      await this.api.receivePurchaseOrder(id, command);
      await Promise.all([this.refreshPurchaseOrders(), this.refreshParts()]);
      // Parts arriving can unblock an order that was short.
      await this.refreshBlocks();
      return id;
    });
  }

  async cancelPurchaseOrder(id: string): Promise<StepResult> {
    return this.write(async () => {
      await this.api.cancelPurchaseOrder(id);
      await this.refreshPurchaseOrders();
      return id;
    });
  }

  async createStockPolicy(command: StockPolicyCommand): Promise<StepResult> {
    return this.write(async () => {
      await this.api.createStockPolicy(command);
      await this.refreshStockPolicies();
      return undefined;
    });
  }

  async updateStockPolicy(id: string, command: StockPolicyCommand): Promise<StepResult> {
    return this.write(async () => {
      await this.api.updateStockPolicy(id, command);
      await this.refreshStockPolicies();
      return id;
    });
  }

  async deleteStockPolicy(id: string): Promise<StepResult> {
    return this.write(async () => {
      await this.api.deleteStockPolicy(id);
      await this.refreshStockPolicies();
      return id;
    });
  }

  /**
   * A worker is created with a password, unlike a customer: they must be able to
   * sign in to this console on their first shift, and nobody else can set it for
   * them afterwards.
   */
  async registerWorker(worker: WorkerCommand, rawPassword: string): Promise<StepResult> {
    return this.write(async () => {
      const created = await this.api.registerWorker(worker, rawPassword);
      await this.refreshWorkers();
      return created.id;
    });
  }

  async updateWorker(id: string, command: CustomerProfileCommand): Promise<StepResult> {
    return this.write(async () => {
      const worker = toWorker(await this.api.updateWorker(id, command));
      this._workers.update((all) => all.map((w) => (w.id === id ? worker : w)));
      this.directory.primeWorkers([worker]);
      this.redecorate();
      return id;
    });
  }

  async terminateWorker(id: string): Promise<StepResult> {
    return this.write(async () => {
      await this.api.terminateWorker(id);
      await this.refreshWorkers();
      return id;
    });
  }

  /* ---------------------------------------------------------------------
     The calendar.
     --------------------------------------------------------------------- */

  async updateSchedulingSettings(command: SchedulingSettingsCommand): Promise<StepResult> {
    return this.write(async () => {
      this._settings.set(toSchedulingSettings(await this.api.updateSchedulingSettings(command)));
      return undefined;
    });
  }

  /** Closing a date cancels the appointments already booked into it. */
  async createClosure(command: ClosureCommand): Promise<StepResult> {
    return this.write(async () => {
      await this.api.createClosure(command);
      await Promise.all([this.refreshClosures(), this.refreshAppointments()]);
      return undefined;
    });
  }

  async deleteClosure(date: string): Promise<StepResult> {
    return this.write(async () => {
      await this.api.deleteClosure(date);
      await this.refreshClosures();
      return date;
    });
  }

  /** The slots still open on a date. Read straight through — never cached. */
  async availability(type: 'DROPOFF' | 'PICKUP', date: string): Promise<string[]> {
    if (this._mode() === 'demo') return [];
    try {
      return await this.api.availability(type, date);
    } catch {
      return [];
    }
  }

  async bookDropoff(command: DropoffOnBehalfCommand): Promise<StepResult> {
    return this.write(async () => {
      const appointment = toAppointment(await this.api.bookDropoffOnBehalf(command));
      await this.directory.resolveForAppointments([appointment]);
      this._appointments.update((all) => [...all, this.directory.decorateAppointment(appointment)]);
      return appointment.id;
    });
  }

  async rescheduleAppointment(id: string, newSlotStart: string): Promise<StepResult> {
    return this.write(async () => {
      this.applyAppointment(this.directory.decorateAppointment(toAppointment(await this.api.reschedule(id, newSlotStart))));
      // A reschedule supersedes the old row; re-read so both states are right.
      await this.refreshAppointments();
      return id;
    });
  }

  /* --- targeted re-reads ------------------------------------------------ */

  private async refreshVendors(): Promise<void> {
    const page = await this.safe(() => this.api.vendors(), 'vendors');
    if (page) this._vendors.set(page.content.map(toVendor));
  }

  private async refreshPurchaseOrders(): Promise<void> {
    const page = await this.safe(() => this.api.purchaseOrders(), 'purchase orders');
    if (page) this._purchaseOrders.set(page.content.map(toPurchaseOrder));
  }

  private async refreshStockPolicies(): Promise<void> {
    const page = await this.safe(() => this.api.stockPolicies(), 'reorder rules');
    if (page) this._reorderRules.set(page.content.map(toReorderRule));
  }

  private async refreshWorkers(): Promise<void> {
    const page = await this.safe(() => this.api.workers(), 'workers');
    if (!page) return;
    const workers = page.content.filter((u) => u.worker).map(toWorker);
    this._workers.set(workers);
    this.directory.primeWorkers(workers);
  }

  private async refreshClosures(): Promise<void> {
    const list = await this.safe(() => this.api.closures(), 'closures');
    if (list) this._closures.set(list.map(toClosure));
  }

  private async refreshAppointments(): Promise<void> {
    const page = await this.safe(() => this.api.appointments(), 'appointments');
    if (!page) return;
    const bookings = page.content.map(toAppointment);
    await this.directory.resolveForAppointments(bookings);
    this._appointments.set(bookings.map((a) => this.directory.decorateAppointment(a)));
  }

  /** Re-read the catalog and its ledger together; they are only true as a pair. */
  private async refreshParts(): Promise<void> {
    const [parts, stock] = await Promise.all([
      this.safe(() => this.api.parts(), 'parts'),
      this.safe(() => this.api.partStock(), 'stock'),
    ]);
    if (!parts) return;
    const stockByPart = new Map((stock?.content ?? []).map((s) => [s.partId, s]));
    this._parts.set(parts.content.map((p) => toPart(p, stockByPart.get(p.id))));
  }

  /* --- cache writes ----------------------------------------------------- */

  private applyWorkOrder(order: WorkOrder): void {
    const decorated = this.directory.decorate(order);
    this._workOrders.update((all) => {
      const index = all.findIndex((o) => o.id === order.id);
      if (index === -1) return [...all, decorated];
      const next = [...all];
      next[index] = decorated;
      return next;
    });
  }

  private applyBudget(budget: Budget): void {
    this._budgets.update((all) => {
      const index = all.findIndex((b) => b.id === budget.id);
      if (index === -1) return [...all, budget];
      const next = [...all];
      next[index] = budget;
      return next;
    });
  }

  private applyAppointment(appointment: Appointment): void {
    this._appointments.update((all) => all.map((a) => (a.id === appointment.id ? appointment : a)));
  }

  /* ---------------------------------------------------------------------
     Demo mutations — the same domain rules, applied in memory.
     --------------------------------------------------------------------- */

  private advanceDemo(order: WorkOrder, target: WorkOrderStatus): StepResult {
    if (target === 'IN_PROGRESS') {
      const block = this.blockFor(order.id);
      if (block.blocked) {
        const names = block.shortfalls.map((s) => `${s.short} × ${s.sku}`).join(', ');
        return { ok: false, error: `Service cannot start: ${names} short on the shelf. Nothing was consumed.` };
      }
      this.consumeReservedStockDemo(order);
    }

    const stamp = new Date().toISOString();
    const timestampField: Partial<Record<WorkOrderStatus, keyof WorkOrder>> = {
      WAITING_DIAGNOSTICS: 'diagnosticRequestedAt',
      IN_DIAGNOSTICS: 'diagnosticStartedAt',
      BUDGET_IN_DRAFT: 'diagnosticFinishedAt',
      IN_PROGRESS: 'serviceStartedAt',
      FINISHED: 'finishedAt',
      WAITING_PICKUP: 'pickupReadyAt',
      DELIVERED: 'deliveredAt',
    };

    this._workOrders.update((orders) =>
      orders.map((o) => {
        if (o.id !== order.id) return o;
        const field = timestampField[target];
        const patch: Partial<WorkOrder> = { status: target, updatedAt: stamp };
        if (field) patch[field] = stamp as never;
        return { ...o, ...patch };
      }),
    );

    if (target === 'WAITING_APPROVAL' && order.budgetId) {
      this._budgets.update((budgets) =>
        budgets.map((b) => (b.id === order.budgetId ? { ...b, status: 'SENT', sentAt: stamp } : b)),
      );
    }

    this.recordDemo(order.id, target, stamp);
    return { ok: true };
  }

  private addLineDemo(
    budgetId: string,
    line: { type: 'PART' | 'SERVICE'; quantity: number; partId?: string; serviceId?: string; description?: string; unitPrice?: number },
  ): void {
    const full: BudgetLine = {
      id: `bl-${Math.random().toString(36).slice(2, 8)}`,
      type: line.type,
      description: line.description ?? '',
      quantity: line.quantity,
      unitPrice: line.unitPrice ?? 0,
      lineTotal: Number((line.quantity * (line.unitPrice ?? 0)).toFixed(2)),
      partId: line.partId ?? null,
      serviceId: line.serviceId ?? null,
      startedAt: null,
      finishedAt: null,
    };
    this._budgets.update((budgets) =>
      budgets.map((b) => (b.id === budgetId && b.status === 'DRAFT' ? retotal({ ...b, lines: [...b.lines, full] }) : b)),
    );
    if (full.partId) this.reserveDemo(full.partId, full.quantity);
  }

  private removeLineDemo(budgetId: string, lineId: string): void {
    const line = this.budget(budgetId)?.lines.find((l) => l.id === lineId);
    this._budgets.update((budgets) =>
      budgets.map((b) =>
        b.id === budgetId && b.status === 'DRAFT'
          ? retotal({ ...b, lines: b.lines.filter((l) => l.id !== lineId) })
          : b,
      ),
    );
    if (line?.partId) this.reserveDemo(line.partId, -line.quantity);
  }

  private setLineQuantityDemo(budgetId: string, lineId: string, quantity: number): void {
    const line = this.budget(budgetId)?.lines.find((l) => l.id === lineId);
    if (!line) return;
    const delta = quantity - line.quantity;
    this._budgets.update((budgets) =>
      budgets.map((b) =>
        b.id === budgetId && b.status === 'DRAFT'
          ? retotal({
              ...b,
              lines: b.lines.map((l) =>
                l.id === lineId ? { ...l, quantity, lineTotal: Number((quantity * l.unitPrice).toFixed(2)) } : l,
              ),
            })
          : b,
      ),
    );
    if (line.partId) this.reserveDemo(line.partId, delta);
  }

  private patchLineDemo(budgetId: string, lineId: string, patch: Partial<BudgetLine>): void {
    this._budgets.update((budgets) =>
      budgets.map((b) =>
        b.id === budgetId ? { ...b, lines: b.lines.map((l) => (l.id === lineId ? { ...l, ...patch } : l)) } : b,
      ),
    );
  }

  /** Move quantity between available and reserved. Never touches on-hand. */
  private reserveDemo(partId: string, delta: number): void {
    this._parts.update((parts) =>
      parts.map((p) =>
        p.id === partId
          ? {
              ...p,
              quantityReserved: p.quantityReserved + delta,
              available: p.quantityOnHand - (p.quantityReserved + delta),
            }
          : p,
      ),
    );
  }

  /** Starting service turns reservations into an append-only consumption. */
  private consumeReservedStockDemo(order: WorkOrder): void {
    const budget = this.budget(order.budgetId);
    if (!budget) return;
    const stamp = new Date().toISOString();
    for (const line of budget.lines) {
      const partId = line.partId;
      if (!partId) continue;
      this._parts.update((parts) =>
        parts.map((p) => {
          if (p.id !== partId) return p;
          const onHand = p.quantityOnHand - line.quantity;
          const reserved = Math.max(0, p.quantityReserved - line.quantity);
          return { ...p, quantityOnHand: onHand, quantityReserved: reserved, available: onHand - reserved };
        }),
      );
      this._movements.update((m) => [
        {
          id: `sm-${Math.random().toString(36).slice(2, 8)}`,
          partId,
          type: 'CONSUMPTION',
          quantity: -line.quantity,
          unitCost: null,
          referenceId: order.id,
          referenceLabel: order.orderCode,
          reason: null,
          occurredAt: stamp,
        },
        ...m,
      ]);
    }
  }

  private recordDemo(workOrderId: string, target: WorkOrderStatus, occurredAt: string): void {
    const step = LIFECYCLE.find((s) => s.status === target);
    this._history.update((h) => ({
      ...h,
      [workOrderId]: [
        {
          id: `h-${Math.random().toString(36).slice(2, 8)}`,
          aggregateType: 'WORK_ORDER',
          aggregateId: workOrderId,
          eventType: `WORK_ORDER_${target}`,
          actorName: null,
          actorIsSystem: false,
          actorIsRole: false,
          occurredAt,
          summary: step?.title ?? 'Step performed',
        },
        ...(h[workOrderId] ?? []),
      ],
    }));
  }
}

function retotal(b: Budget): Budget {
  return { ...b, grandTotal: Number(b.lines.reduce((s, l) => s + l.lineTotal, 0).toFixed(2)) };
}

/** The demo timelines are authored oldest-first; every view reads newest-first. */
function reverseAll(history: Record<string, HistoryEntry[]>): Record<string, HistoryEntry[]> {
  return Object.fromEntries(Object.entries(history).map(([id, entries]) => [id, [...entries].reverse()]));
}

function describe(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  return 'That step could not be performed.';
}
