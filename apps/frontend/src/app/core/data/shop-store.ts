import { Injectable, computed, signal } from '@angular/core';
import type { WorkOrderStatus } from '../domain/enums';
import { LIFECYCLE, nextStep, stepIndex } from '../domain/lifecycle';
import type {
  Appointment,
  Budget,
  BudgetLine,
  Customer,
  HistoryEntry,
  Part,
  PurchaseOrder,
  RepairService,
  ReorderRule,
  StockMovement,
  Vehicle,
  Vendor,
  Worker,
  WorkOrder,
  WorkOrderBlock,
} from '../domain/models';
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

/**
 * The shop's state, in one place.
 *
 * In demo mode this is an in-memory repository seeded from `demo-data.ts`, and
 * every mutation below applies the same rules the backend applies — a sent
 * budget freezes, a draft edit moves a reservation, starting service consumes
 * stock and writes a movement. Against the live API each method becomes an HTTP
 * call and the signals become the response cache; the shapes do not change.
 */
@Injectable({ providedIn: 'root' })
export class ShopStore {
  readonly now = DEMO_NOW;

  private readonly _workOrders = signal<WorkOrder[]>(structuredClone(DEMO_WORK_ORDERS));
  private readonly _budgets = signal<Budget[]>(structuredClone(DEMO_BUDGETS));
  private readonly _parts = signal<Part[]>(structuredClone(DEMO_PARTS));
  private readonly _blocks = signal<WorkOrderBlock[]>(structuredClone(DEMO_BLOCKS));
  private readonly _appointments = signal<Appointment[]>(structuredClone(DEMO_APPOINTMENTS));
  private readonly _movements = signal<StockMovement[]>(structuredClone(DEMO_STOCK_MOVEMENTS));
  private readonly _history = signal<Record<string, HistoryEntry[]>>(structuredClone(DEMO_HISTORY));

  readonly workOrders = this._workOrders.asReadonly();
  readonly budgets = this._budgets.asReadonly();
  readonly parts = this._parts.asReadonly();
  readonly appointments = this._appointments.asReadonly();
  readonly movements = this._movements.asReadonly();

  readonly services = signal<RepairService[]>(structuredClone(DEMO_SERVICES)).asReadonly();
  readonly vendors = signal<Vendor[]>(structuredClone(DEMO_VENDORS)).asReadonly();
  readonly purchaseOrders = signal<PurchaseOrder[]>(structuredClone(DEMO_PURCHASE_ORDERS)).asReadonly();
  readonly reorderRules = signal<ReorderRule[]>(structuredClone(DEMO_REORDER_RULES)).asReadonly();
  readonly customers = signal<Customer[]>(structuredClone(DEMO_CUSTOMERS)).asReadonly();
  readonly vehicles = signal<Vehicle[]>(structuredClone(DEMO_VEHICLES)).asReadonly();
  readonly workers = signal<Worker[]>(structuredClone(DEMO_WORKERS)).asReadonly();
  readonly closures = signal(structuredClone(DEMO_CLOSURES)).asReadonly();
  readonly settings = signal(structuredClone(DEMO_SETTINGS)).asReadonly();

  /** Live jobs — everything that has not left the shop. */
  readonly liveWorkOrders = computed(() =>
    this._workOrders()
      .filter((o) => o.status !== 'DELIVERED' && o.status !== 'REFUSED')
      .sort((a, b) => stepIndex(a.status) - stepIndex(b.status) || a.orderCode.localeCompare(b.orderCode)),
  );

  readonly closedWorkOrders = computed(() =>
    this._workOrders().filter((o) => o.status === 'DELIVERED' || o.status === 'REFUSED'),
  );

  /** Count per status, for the board's section rule. */
  readonly statusCounts = computed(() => {
    const counts = new Map<WorkOrderStatus, number>();
    for (const o of this._workOrders()) counts.set(o.status, (counts.get(o.status) ?? 0) + 1);
    return counts;
  });

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

  customer(id: string): Customer | undefined {
    return this.customers().find((c) => c.id === id);
  }

  vehicle(id: string): Vehicle | undefined {
    return this.vehicles().find((v) => v.id === id);
  }

  /**
   * Whether this order is blocked, and by what.
   *
   * Brief §7.1: the backend has no shortfall read endpoint yet. This is the
   * adapter — against the live API an absent endpoint yields `blocked: false`
   * and an empty list, so the board degrades to showing nothing rather than
   * claiming everything is fine. The reactive path (a 409 on `service/start`)
   * remains the authority until the endpoint ships.
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

  readonly blockedCount = computed(() => this._blocks().filter((b) => b.blocked).length);

  history(workOrderId: string): HistoryEntry[] {
    const authored = this._history()[workOrderId];
    if (authored) return [...authored].reverse();
    const order = this.workOrder(workOrderId);
    return order ? fallbackHistory(order) : [];
  }

  /* ---------------------------------------------------------------------
     Mutations
     --------------------------------------------------------------------- */

  /** Perform the next lawful step of the procedure. */
  advance(workOrderId: string): { ok: boolean; error?: string } {
    const order = this.workOrder(workOrderId);
    if (!order) return { ok: false, error: 'Work order not found.' };

    const step = nextStep(order.status);
    if (!step) return { ok: false, error: 'This work order has reached the end of the procedure.' };

    // Step 7 is guarded by stock. A shortfall blocks it and consumes nothing.
    if (step.status === 'IN_PROGRESS') {
      const block = this.blockFor(workOrderId);
      if (block.blocked) {
        const names = block.shortfalls.map((s) => `${s.short} × ${s.sku}`).join(', ');
        return {
          ok: false,
          error: `Service cannot start: ${names} short on the shelf. Nothing was consumed.`,
        };
      }
      this.consumeReservedStock(order);
    }

    const stamp = new Date().toISOString();
    const timestampField: Partial<Record<WorkOrderStatus, keyof WorkOrder>> = {
      WAITING_DIAGNOSTICS: 'diagnosticRequestedAt',
      IN_DIAGNOSTICS: 'diagnosticStartedAt',
      BUDGET_IN_DRAFT: 'diagnosticFinishedAt',
      WAITING_APPROVAL: 'approvedAt',
      IN_PROGRESS: 'serviceStartedAt',
      FINISHED: 'finishedAt',
      WAITING_PICKUP: 'pickupReadyAt',
      DELIVERED: 'deliveredAt',
    };

    this._workOrders.update((orders) =>
      orders.map((o) => {
        if (o.id !== workOrderId) return o;
        const field = timestampField[step.status];
        const patch: Partial<WorkOrder> = { status: step.status, updatedAt: stamp };
        // WAITING_APPROVAL records the send time on the budget, not approvedAt.
        if (field && step.status !== 'WAITING_APPROVAL') patch[field] = stamp as never;
        return { ...o, ...patch };
      }),
    );

    if (step.status === 'WAITING_APPROVAL') this.freezeAndSend(order.budgetId, stamp);
    this.record(workOrderId, step.title, stamp);
    return { ok: true };
  }

  /** Sending a budget freezes its lines permanently. */
  private freezeAndSend(budgetId: string | null, stamp: string): void {
    if (!budgetId) return;
    this._budgets.update((budgets) =>
      budgets.map((b) => (b.id === budgetId ? { ...b, status: 'SENT', sentAt: stamp, lastSendError: null } : b)),
    );
  }

  /** A budget stuck in WAITING_SEND may be resent as often as needed. */
  resendBudget(budgetId: string): void {
    const stamp = new Date().toISOString();
    this._budgets.update((budgets) =>
      budgets.map((b) => (b.id === budgetId ? { ...b, status: 'SENT', sentAt: stamp, lastSendError: null } : b)),
    );
    const b = this.budget(budgetId);
    if (b) this.record(b.workOrderId, 'Budget resent to the customer', stamp);
  }

  /**
   * Draft-only. Every add, removal and quantity change reserves or releases
   * stock immediately — the consequence is applied here so the UI can show it
   * in the same view as the action.
   */
  addLine(budgetId: string, line: Omit<BudgetLine, 'id' | 'lineTotal'>): void {
    const id = `bl-${Math.random().toString(36).slice(2, 8)}`;
    const full: BudgetLine = { ...line, id, lineTotal: Number((line.quantity * line.unitPrice).toFixed(2)) };
    this._budgets.update((budgets) =>
      budgets.map((b) => (b.id === budgetId && b.status === 'DRAFT' ? this.retotal({ ...b, lines: [...b.lines, full] }) : b)),
    );
    if (full.partId) this.reserve(full.partId, full.quantity);
  }

  removeLine(budgetId: string, lineId: string): void {
    const budget = this.budget(budgetId);
    const line = budget?.lines.find((l) => l.id === lineId);
    this._budgets.update((budgets) =>
      budgets.map((b) =>
        b.id === budgetId && b.status === 'DRAFT'
          ? this.retotal({ ...b, lines: b.lines.filter((l) => l.id !== lineId) })
          : b,
      ),
    );
    if (line?.partId) this.reserve(line.partId, -line.quantity);
  }

  setLineQuantity(budgetId: string, lineId: string, quantity: number): void {
    if (quantity < 1) return;
    const budget = this.budget(budgetId);
    const line = budget?.lines.find((l) => l.id === lineId);
    if (!line) return;
    const delta = quantity - line.quantity;
    this._budgets.update((budgets) =>
      budgets.map((b) =>
        b.id === budgetId && b.status === 'DRAFT'
          ? this.retotal({
              ...b,
              lines: b.lines.map((l) =>
                l.id === lineId ? { ...l, quantity, lineTotal: Number((quantity * l.unitPrice).toFixed(2)) } : l,
              ),
            })
          : b,
      ),
    );
    if (line.partId) this.reserve(line.partId, delta);
  }

  /** Mechanic timing on a single service line. */
  startLine(budgetId: string, lineId: string): void {
    const stamp = new Date().toISOString();
    this.patchLine(budgetId, lineId, { startedAt: stamp });
  }

  finishLine(budgetId: string, lineId: string): void {
    const stamp = new Date().toISOString();
    this.patchLine(budgetId, lineId, { finishedAt: stamp });
  }

  private patchLine(budgetId: string, lineId: string, patch: Partial<BudgetLine>): void {
    this._budgets.update((budgets) =>
      budgets.map((b) =>
        b.id === budgetId ? { ...b, lines: b.lines.map((l) => (l.id === lineId ? { ...l, ...patch } : l)) } : b,
      ),
    );
  }

  private retotal(b: Budget): Budget {
    return { ...b, grandTotal: Number(b.lines.reduce((s, l) => s + l.lineTotal, 0).toFixed(2)) };
  }

  /** Move quantity between available and reserved. Never touches on-hand. */
  private reserve(partId: string, delta: number): void {
    this._parts.update((parts) =>
      parts.map((p) =>
        p.id === partId
          ? { ...p, quantityReserved: p.quantityReserved + delta, available: p.quantityOnHand - (p.quantityReserved + delta) }
          : p,
      ),
    );
  }

  /** Starting service turns reservations into an append-only consumption. */
  private consumeReservedStock(order: WorkOrder): void {
    const budget = this.budget(order.budgetId);
    if (!budget) return;
    const stamp = new Date().toISOString();
    for (const line of budget.lines) {
      if (!line.partId) continue;
      this._parts.update((parts) =>
        parts.map((p) =>
          p.id === line.partId
            ? {
                ...p,
                quantityOnHand: p.quantityOnHand - line.quantity,
                quantityReserved: Math.max(0, p.quantityReserved - line.quantity),
                available: p.quantityOnHand - line.quantity - Math.max(0, p.quantityReserved - line.quantity),
              }
            : p,
        ),
      );
      const part = this.part(line.partId);
      this._movements.update((m) => [
        {
          id: `sm-${Math.random().toString(36).slice(2, 8)}`,
          partId: line.partId!,
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
      void part;
    }
  }

  /** Checking in a drop-off produces a work order; a pickup delivers one. */
  checkIn(appointmentId: string): void {
    const stamp = new Date().toISOString();
    this._appointments.update((list) =>
      list.map((a) => (a.id === appointmentId ? { ...a, status: 'COMPLETED', checkedInAt: stamp, updatedAt: stamp } : a)),
    );
  }

  cancelAppointment(appointmentId: string, message: string | null): void {
    const stamp = new Date().toISOString();
    this._appointments.update((list) =>
      list.map((a) =>
        a.id === appointmentId
          ? { ...a, status: 'CANCELLED', cancelReason: 'STAFF_REQUESTED', cancelMessage: message, updatedAt: stamp }
          : a,
      ),
    );
  }

  private record(workOrderId: string, summary: string, occurredAt: string): void {
    this._history.update((h) => {
      const existing = h[workOrderId] ?? [];
      return {
        ...h,
        [workOrderId]: [
          ...existing,
          {
            id: `h-${Math.random().toString(36).slice(2, 8)}`,
            aggregateType: 'WORK_ORDER',
            aggregateId: workOrderId,
            eventType: 'StatusChanged',
            actorName: null,
            actorIsSystem: false,
            customerVisible: true,
            occurredAt,
            summary,
          },
        ],
      };
    });
  }

  /** Used by the board's section rule. */
  readonly lifecycle = LIFECYCLE;
}
