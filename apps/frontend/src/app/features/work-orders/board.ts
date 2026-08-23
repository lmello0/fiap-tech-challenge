import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ShopStore } from '../../core/data/shop-store';
import { Session } from '../../core/auth/session';
import type { WorkOrder, WorkOrderBlock } from '../../core/domain/models';
import type { WorkOrderStatus } from '../../core/domain/enums';
import { LIFECYCLE, mayAdvance, nextStep, stepFor } from '../../core/domain/lifecycle';
import { StepRail } from '../../shared/ui/step-rail';
import { StatusMark } from '../../shared/ui/status-mark';
import { Callout } from '../../shared/ui/callout';
import { Icon } from '../../shared/ui/icon';

export interface BoardRow {
  order: WorkOrder;
  block: WorkOrderBlock;
  /** Days the vehicle has been in the shop. */
  age: number;
  /** Days the budget has sat with the customer, when it has. */
  waitingDays: number | null;
  nextLabel: string | null;
  canAdvance: boolean;
  /** Why the next step is unavailable to this operator, when it is. */
  blockedReason: string | null;
  /** The short word for that reason, printed in the action column. */
  inertWord: string | null;
  inertIcon: string;
}

@Component({
  selector: 'app-work-order-board',
  imports: [RouterLink, StepRail, StatusMark, Callout, Icon],
  templateUrl: './board.html',
  styleUrl: './board.scss',
})
export class WorkOrderBoard {
  private readonly store = inject(ShopStore);
  protected readonly session = inject(Session);

  protected readonly lifecycle = LIFECYCLE;

  protected readonly query = signal('');
  protected readonly statusFilter = signal<WorkOrderStatus | 'ALL'>('ALL');
  /** A mechanic opens the board on their own jobs; anyone may widen it. */
  protected readonly mineOnly = signal(this.session.role() === 'MECHANIC');
  protected readonly showClosed = signal(false);

  protected readonly toast = signal<{ tone: 'warn' | 'ok'; text: string } | null>(null);

  /**
   * True only while the table is genuinely scrolled sideways. Drives the cue on
   * the pinned action column, so an operator can see that columns pass beneath
   * it — and so that no cue is drawn at widths where nothing is hidden.
   */
  protected readonly scrolled = signal(false);

  protected onScroll(event: Event): void {
    this.scrolled.set((event.target as HTMLElement).scrollLeft > 1);
  }

  protected readonly statuses = computed(() => {
    const counts = this.store.statusCounts();
    return LIFECYCLE.map((s) => ({ status: s.status, title: s.title, count: counts.get(s.status) ?? 0 })).concat([
      { status: 'REFUSED' as WorkOrderStatus, title: 'Refused', count: counts.get('REFUSED') ?? 0 },
    ]);
  });

  protected readonly rows = computed<BoardRow[]>(() => {
    const q = this.query().trim().toLowerCase();
    const status = this.statusFilter();
    const mine = this.mineOnly();
    const me = this.session.worker();
    const role = this.session.role();

    const source = this.showClosed()
      ? [...this.store.liveWorkOrders(), ...this.store.closedWorkOrders()]
      : this.store.liveWorkOrders();

    return source
      .filter((o) => (status === 'ALL' ? true : o.status === status))
      .filter((o) => (mine && me ? o.assignedMechanicId === me.id : true))
      .filter((o) =>
        q
          ? [o.orderCode, o.customerName, o.vehiclePlate, o.vehicleLabel]
              .filter(Boolean)
              .some((v) => v!.toLowerCase().includes(q))
          : true,
      )
      .map((order) => {
        const block = this.store.blockFor(order.id);
        const step = nextStep(order.status);
        const allowed = mayAdvance(order.status, role);

        // Three genuinely different reasons a row offers no action, and each
        // one says which it is. "Not yours" for a job nobody is waiting on you
        // for would be a lie the operator has to decode.
        let blockedReason: string | null = null;
        let inertWord: string | null = null;
        let inertIcon = 'lock';

        if (step?.action && !allowed) {
          blockedReason = `Only a ${step.action.roles.map((r) => r.toLowerCase()).join(' or ')} may perform this step.`;
          inertWord = 'Not yours';
          inertIcon = 'lock';
        }
        if (order.status === 'WAITING_APPROVAL') {
          blockedReason = 'Waiting on the customer. No staff action can approve on their behalf.';
          inertWord = 'With customer';
          inertIcon = 'clock';
        }
        if (order.status === 'APPROVED' && block.blocked) {
          blockedReason = 'Blocked by a stock shortfall — nothing has been consumed.';
          inertWord = 'Blocked';
          inertIcon = 'alert';
        }

        return {
          inertWord,
          inertIcon,
          order,
          block,
          age: this.daysSince(order.createdAt),
          waitingDays:
            order.status === 'WAITING_APPROVAL' ? this.daysSince(this.store.budget(order.budgetId)?.sentAt ?? order.updatedAt) : null,
          nextLabel: step?.action?.label ?? null,
          canAdvance: allowed && !(block.blocked && step?.status === 'IN_PROGRESS'),
          blockedReason,
        };
      });
  });

  protected readonly blockedRows = computed(() => this.rows().filter((r) => r.block.blocked));

  protected readonly waitingOnCustomer = computed(
    () => this.rows().filter((r) => r.order.status === 'WAITING_APPROVAL').length,
  );

  protected advance(row: BoardRow): void {
    const result = this.store.advance(row.order.id);
    this.toast.set(
      result.ok
        ? { tone: 'ok', text: `${row.order.orderCode} advanced to ${stepFor(this.store.workOrder(row.order.id)!.status).title.toLowerCase()}.` }
        : { tone: 'warn', text: result.error ?? 'That step could not be performed.' },
    );
    setTimeout(() => this.toast.set(null), 6000);
  }

  protected clearFilters(): void {
    this.query.set('');
    this.statusFilter.set('ALL');
    this.mineOnly.set(false);
    this.showClosed.set(false);
  }

  protected firstName(full: string | null | undefined): string {
    return full ? full.split(' ')[0] : '—';
  }

  protected daysSince(iso: string): number {
    const then = new Date(iso).getTime();
    return Math.max(0, Math.floor((this.store.now.getTime() - then) / 86_400_000));
  }

  protected onQuery(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
  }

  protected onStatus(event: Event): void {
    this.statusFilter.set((event.target as HTMLSelectElement).value as WorkOrderStatus | 'ALL');
  }
}
