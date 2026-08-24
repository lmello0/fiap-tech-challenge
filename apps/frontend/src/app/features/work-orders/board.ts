import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormField, form, minLength, required, schema } from '@angular/forms/signals';
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
import { EntryBand } from '../../shared/ui/entry-band';
import { FormFieldRow } from '../../shared/ui/form-field';

interface OpenDraft {
  customerId: string;
  vehicleId: string;
  complaint: string;
}

const EMPTY_OPEN: OpenDraft = { customerId: '', vehicleId: '', complaint: '' };

const openSchema = schema<OpenDraft>((p) => {
  required(p.customerId, { message: 'Whose vehicle is it?' });
  required(p.vehicleId, { message: 'Which vehicle came in?' });
  required(p.complaint, { message: 'Record what the customer reported.' });
  minLength(p.complaint, 5, { message: 'A sentence, in the customer’s own terms.' });
});

/**
 * Which column the lookup term is matched against.
 *
 * The API ANDs every filter field, so one term cannot be tried against several
 * columns at once — asking for a plate *and* a customer name returns nothing.
 * The operator therefore says which column they are looking up, the way an index
 * at the back of a manual is looked up by one of its own columns.
 */
type LookupField = 'vehiclePlate' | 'customerName' | 'code' | 'vehicleMake' | 'vehicleModel' | 'mechanicName';

const LOOKUP_FIELDS: { id: LookupField; label: string; example: string }[] = [
  { id: 'vehiclePlate', label: 'Plate', example: 'ABC1D23' },
  { id: 'customerName', label: 'Customer', example: 'Name, or part of one' },
  { id: 'code', label: 'Order code', example: 'WO-20260824-000001' },
  { id: 'vehicleMake', label: 'Make', example: 'Volkswagen' },
  { id: 'vehicleModel', label: 'Model', example: 'Gol 1.6 MSI' },
  { id: 'mechanicName', label: 'Mechanic', example: 'Name, or part of one' },
];

/** Everything that has not left the shop — the board's default set. */
const LIVE_STATUSES: WorkOrderStatus[] = [
  'RECEIVED',
  'WAITING_DIAGNOSTICS',
  'IN_DIAGNOSTICS',
  'BUDGET_IN_DRAFT',
  'WAITING_APPROVAL',
  'APPROVED',
  'IN_PROGRESS',
  'FINISHED',
  'WAITING_PICKUP',
];

const CLOSED_STATUSES: WorkOrderStatus[] = ['DELIVERED', 'REFUSED'];

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
  imports: [RouterLink, StepRail, StatusMark, Callout, Icon, EntryBand, FormFieldRow, FormField],
  templateUrl: './board.html',
  styleUrl: './board.scss',
})
export class WorkOrderBoard {
  private readonly store = inject(ShopStore);
  protected readonly session = inject(Session);

  protected readonly lifecycle = LIFECYCLE;

  protected readonly query = signal('');
  protected readonly lookupField = signal<LookupField>('vehiclePlate');
  protected readonly lookupFields = LOOKUP_FIELDS;
  protected readonly statusFilter = signal<WorkOrderStatus | 'ALL'>('ALL');
  /** A mechanic opens the board on their own jobs; anyone may widen it. */
  protected readonly mineOnly = signal(this.session.role() === 'MECHANIC');
  protected readonly showClosed = signal(false);

  protected readonly toast = signal<{ tone: 'warn' | 'ok'; text: string } | null>(null);

  /** True while the counts come from the database rather than the loaded page. */
  protected readonly countsAreExact = this.store.countsAreExact;
  protected readonly loading = this.store.loading;

  constructor() {
    // The query belongs to the API, so a change to it re-runs the request rather
    // than re-filtering what happens to be in hand. Debounced: an operator types
    // a plate a character at a time and should not spend a request per keystroke.
    effect((onCleanup) => {
      const filter = this.serverFilter();
      if (this.store.isDemo()) return;
      const handle = setTimeout(() => void this.store.loadWorkOrders(filter), 250);
      onCleanup(() => clearTimeout(handle));
    });
  }

  /**
   * What the API is asked for — the whole query.
   *
   * The board narrows nothing itself against a live API: the step filter, the
   * lookup and the closed toggle are all the database's work, which is what
   * keeps it right once the shop outgrows a single request.
   */
  private readonly serverFilter = computed(() => {
    const status = this.statusFilter();
    const term = this.query().trim();
    const field = this.lookupField();

    return {
      status:
        status !== 'ALL'
          ? [status]
          : this.showClosed()
            ? [...LIVE_STATUSES, ...CLOSED_STATUSES]
            : LIVE_STATUSES,
      ...(term ? { [field]: normaliseTerm(field, term) } : {}),
    };
  });

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
    const mine = this.mineOnly();
    const me = this.session.worker();
    const role = this.session.role();

    // Asking for a closed step is asking to see closed jobs: the "include
    // closed" toggle widens the default view, it does not veto an explicit
    // choice. Without this, picking Delivered returns rows from the API and
    // then hides every one of them.
    const source =
      this.statusFilter() !== 'ALL'
        ? this.store.liveWorkOrders().concat(this.store.closedWorkOrders())
        : this.store.liveWorkOrders().concat(this.showClosed() ? this.store.closedWorkOrders() : []);

    // Demo mode has no API behind it, so the same narrowing is applied here over
    // the synthetic shop; against the API this pass does nothing. "My jobs" is
    // always local — it filters on the signed-in worker, and re-querying for an
    // instant toggle is not worth the round trip.
    const demo = this.store.isDemo();
    const status = this.statusFilter();
    const term = this.query().trim().toLowerCase();
    const field = this.lookupField();

    return source
      .filter((o) => (mine && me ? o.assignedMechanicId === me.id : true))
      .filter((o) => (demo && status !== 'ALL' ? o.status === status : true))
      .filter((o) => (demo && term ? this.matchesLocally(o, field, term) : true))
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

  protected async advance(row: BoardRow): Promise<void> {
    const result = await this.store.advance(row.order.id);
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

  protected onLookupField(event: Event): void {
    this.lookupField.set((event.target as HTMLSelectElement).value as LookupField);
  }

  protected readonly lookupHint = 'Matches any part of the value.';

  private readonly lookup = computed(
    () => LOOKUP_FIELDS.find((f) => f.id === this.lookupField()) ?? LOOKUP_FIELDS[0],
  );

  protected readonly lookupLabel = computed(() => this.lookup().label);

  /** The placeholder shows what a value looks like, not the label again. */
  protected readonly lookupExample = computed(() => this.lookup().example);

  /** Whether any filter is narrowing the board, for the empty state's wording. */
  protected readonly isFiltered = computed(
    () => this.query().trim() !== '' || this.statusFilter() !== 'ALL' || this.mineOnly(),
  );

  /** The same column the API would have matched on, matched here instead. */
  private matchesLocally(o: WorkOrder, field: LookupField, term: string): boolean {
    const value =
      field === 'vehiclePlate'
        ? o.vehiclePlate
        : field === 'customerName'
          ? o.customerName
          : field === 'code'
            ? o.orderCode
            : field === 'mechanicName'
              ? o.assignedMechanicName
              : o.vehicleLabel;
    return (value ?? '').toLowerCase().includes(term);
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

  /* ---------------------------------------------------------------------
     Opening a work order.

     Step 1 of the procedure, and the only step that creates rather than moves.
     It is written at the head of the board because that is where the job will
     appear a moment later.
     --------------------------------------------------------------------- */

  protected readonly isDemo = this.store.isDemo;
  protected readonly canOpen = computed(() => this.session.hasAnyRole('ATTENDANT', 'MANAGER'));

  protected readonly opening = signal(false);
  protected readonly busy = signal(false);
  protected readonly bandError = signal<string | null>(null);

  protected readonly draft = signal<OpenDraft>({ ...EMPTY_OPEN });
  protected readonly f = form(this.draft, openSchema);

  protected readonly customers = computed(() =>
    this.store
      .customers()
      .filter((c) => c.active)
      .sort((a, b) => a.name.localeCompare(b.name)),
  );

  /** Only the chosen customer's own vehicles — a work order cannot cross owners. */
  protected readonly ownedVehicles = computed(() => {
    const owner = this.draft().customerId;
    return owner ? this.store.vehicles().filter((v) => v.customerId === owner && v.active) : [];
  });

  protected readonly canStartIntake = computed(() => this.customers().length > 0);

  protected openIntake(): void {
    this.draft.set({ ...EMPTY_OPEN, customerId: this.customers()[0]?.id ?? '' });
    this.bandError.set(null);
    this.opening.set(true);
  }

  protected closeIntake(): void {
    this.opening.set(false);
    this.bandError.set(null);
  }

  /** Changing the owner invalidates a vehicle chosen under the previous one. */
  protected onOwnerChange(): void {
    this.draft.update((d) => ({ ...d, vehicleId: '' }));
  }

  protected async openWorkOrder(): Promise<void> {
    if (this.f().invalid()) return;
    this.busy.set(true);
    this.bandError.set(null);
    const d = this.draft();
    const result = await this.store.createWorkOrder(d.customerId, d.vehicleId, d.complaint.trim());
    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That work order could not be opened.');
      return;
    }
    this.closeIntake();
    this.toast.set({ tone: 'ok', text: 'Work order opened. It stands at step 1, vehicle received.' });
    setTimeout(() => this.toast.set(null), 6000);
  }
}

/**
 * The term as the column stores it.
 *
 * Matching is case-insensitive, so only punctuation needs correcting: plates are
 * stored bare, and an operator reading one off a document may well type the
 * separator that is printed on it.
 */
function normaliseTerm(field: LookupField, term: string): string {
  return field === 'vehiclePlate' ? term.replace(/[^A-Za-z0-9]/g, '') : term;
}
