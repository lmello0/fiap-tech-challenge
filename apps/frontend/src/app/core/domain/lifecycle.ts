import type { WorkOrderStatus, WorkerRole } from './enums';

/**
 * The work order lifecycle, written as a manual procedure.
 *
 * This is the spine of the console. Every step below is transcribed from the
 * backend's own truth: `WorkOrderStatus` supplies the steps, the
 * `@PreAuthorize` annotations on `WorkOrderController` and `BudgetController`
 * supply `roles`, and `CONTEXT.md` supplies the preconditions and consequences.
 * Nothing here is invented — if a rule changes in the backend it changes here,
 * and the step rail, the row actions and the permission notices all follow.
 */

export type CalloutTier = 'warning' | 'caution' | 'note';

export interface LifecycleAction {
  /** Imperative, in the shop's own language. */
  readonly label: string;
  /** The endpoint this step performs, shown in the procedure detail. */
  readonly endpoint: string;
  /** Roles the backend will actually accept. */
  readonly roles: readonly WorkerRole[];
  /** Raised before the action is confirmed when it is frozen or terminal. */
  readonly tier?: CalloutTier;
  readonly consequence?: string;
}

export interface LifecycleStep {
  readonly n: number;
  readonly status: WorkOrderStatus;
  readonly title: string;
  /** What must already be true for this step to be lawful. */
  readonly precondition: string;
  /** The action that moves an order INTO this step. */
  readonly action?: LifecycleAction;
  /** Who the shop is waiting on while an order rests here. */
  readonly waitingOn?: 'customer' | 'mechanic' | 'counter';
}

/** The main line: ten steps from intake to delivery. */
export const LIFECYCLE: readonly LifecycleStep[] = [
  {
    n: 1,
    status: 'RECEIVED',
    title: 'Vehicle received',
    precondition:
      'A drop-off appointment has been checked in, or an attendant opened the order at the counter.',
    action: {
      label: 'Open work order',
      endpoint: 'POST /work-orders',
      roles: ['ATTENDANT', 'MANAGER'],
    },
    waitingOn: 'counter',
  },
  {
    n: 2,
    status: 'WAITING_DIAGNOSTICS',
    title: 'Queued for diagnostics',
    precondition: 'The vehicle is on the premises and the complaint has been recorded.',
    action: {
      label: 'Request diagnostics',
      endpoint: 'POST /work-orders/{id}/diagnostics/request',
      roles: ['ATTENDANT', 'MANAGER'],
    },
    waitingOn: 'mechanic',
  },
  {
    n: 3,
    status: 'IN_DIAGNOSTICS',
    title: 'Under diagnosis',
    precondition: 'A mechanic has taken the job. Diagnosis is the inspection, not the repair.',
    action: {
      label: 'Start diagnostics',
      endpoint: 'POST /work-orders/{id}/diagnostics/start',
      roles: ['MECHANIC', 'MANAGER'],
    },
    waitingOn: 'mechanic',
  },
  {
    n: 4,
    status: 'BUDGET_IN_DRAFT',
    title: 'Budget in draft',
    precondition:
      'Diagnosis is recorded. Finishing diagnostics opens the budget draft, seeded with the mechanic’s initial lines.',
    action: {
      label: 'Finish diagnostics',
      endpoint: 'POST /work-orders/{id}/diagnostics/finish',
      roles: ['MECHANIC', 'MANAGER'],
      tier: 'note',
      consequence:
        'Every line added, removed or re-quantified while in draft reserves or releases stock immediately.',
    },
    waitingOn: 'mechanic',
  },
  {
    n: 5,
    status: 'WAITING_APPROVAL',
    title: 'Sent for approval',
    precondition: 'The draft carries at least one line and the customer has a reachable email.',
    action: {
      label: 'Send budget',
      endpoint: 'POST /budgets/{id}/send',
      roles: ['ATTENDANT', 'MANAGER'],
      tier: 'warning',
      consequence:
        'Sending freezes this budget’s lines permanently. There is no requoting — the customer may only approve or refuse.',
    },
    waitingOn: 'customer',
  },
  {
    n: 6,
    status: 'APPROVED',
    title: 'Approved by customer',
    precondition:
      'The customer approved the budget from their own copy. No staff action can approve on their behalf.',
    waitingOn: 'mechanic',
  },
  {
    n: 7,
    status: 'IN_PROGRESS',
    title: 'Service under way',
    precondition:
      'Every reserved part is on the shelf. A shortfall blocks this step and consumes nothing.',
    action: {
      label: 'Start service',
      endpoint: 'POST /work-orders/{id}/service/start',
      roles: ['MECHANIC', 'MANAGER'],
      tier: 'warning',
      consequence:
        'Starting service consumes the reserved stock. The movement is append-only and cannot be reversed except by an adjustment.',
    },
    waitingOn: 'mechanic',
  },
  {
    n: 8,
    status: 'FINISHED',
    title: 'Service finished',
    precondition: 'Every service line on the approved budget has been started and finished.',
    action: {
      label: 'Finish service',
      endpoint: 'POST /work-orders/{id}/service/finish',
      roles: ['MECHANIC', 'MANAGER'],
    },
    waitingOn: 'counter',
  },
  {
    n: 9,
    status: 'WAITING_PICKUP',
    title: 'Ready for pickup',
    precondition: 'The work is finished and the vehicle is ready to leave.',
    action: {
      label: 'Mark ready for pickup',
      endpoint: 'POST /work-orders/{id}/pickup-ready',
      roles: ['ATTENDANT', 'MANAGER'],
      tier: 'note',
      consequence:
        'This sends a booking invitation only. No pickup appointment exists until the customer picks a slot.',
    },
    waitingOn: 'customer',
  },
  {
    n: 10,
    status: 'DELIVERED',
    title: 'Delivered',
    precondition: 'The customer collected the vehicle, normally by checking in a pickup appointment.',
    action: {
      label: 'Record delivery',
      endpoint: 'POST /work-orders/{id}/delivery',
      roles: ['ATTENDANT', 'MANAGER'],
    },
  },
];

/** The one branch off the main line. Terminal — there is no way back. */
export const REFUSED_STEP: LifecycleStep = {
  n: 5,
  status: 'REFUSED',
  title: 'Budget refused',
  precondition:
    'The customer refused the sent budget. This ends the work order — a refused budget is terminal and cannot be requoted.',
  waitingOn: undefined,
};

/**
 * The other branch off the main line. Unlike REFUSED, cancellation has no fixed
 * position — the backend allows it from any status before WAITING_PICKUP is left
 * behind (see `WorkOrderStateMachine`), so `n` is left at 0 rather than claiming
 * a step it did not necessarily pass through.
 */
export const CANCELLED_STEP: LifecycleStep = {
  n: 0,
  status: 'CANCELLED',
  title: 'Cancelled',
  precondition: 'A staff member cancelled this work order. This ends the job — it cannot be reopened.',
  waitingOn: undefined,
};

const BY_STATUS = new Map<WorkOrderStatus, LifecycleStep>([
  ...LIFECYCLE.map((s) => [s.status, s] as const),
  ['REFUSED', REFUSED_STEP] as const,
  ['CANCELLED', CANCELLED_STEP] as const,
]);

export function stepFor(status: WorkOrderStatus): LifecycleStep {
  return BY_STATUS.get(status)!;
}

/** Position on the main line, 1-based. REFUSED reports where it branched. */
export function stepIndex(status: WorkOrderStatus): number {
  return stepFor(status).n;
}

export function isTerminal(status: WorkOrderStatus): boolean {
  return status === 'REFUSED' || status === 'DELIVERED' || status === 'CANCELLED';
}

/** The step an order would move to next, or null at a terminal or waiting state. */
export function nextStep(status: WorkOrderStatus): LifecycleStep | null {
  if (isTerminal(status)) return null;
  const here = stepIndex(status);
  return LIFECYCLE.find((s) => s.n === here + 1) ?? null;
}

/**
 * Whether this role may perform the step that advances an order out of
 * `status`. Mirrors the backend; the UI never offers what the API would refuse.
 */
export function mayAdvance(status: WorkOrderStatus, role: WorkerRole | null): boolean {
  const next = nextStep(status);
  if (!next?.action || !role) return false;
  return next.action.roles.includes(role);
}

/** Roles the backend accepts on `POST /work-orders/{id}/cancel`. */
export const CANCEL_ROLES: readonly WorkerRole[] = ['ATTENDANT', 'MANAGER'];

/**
 * Whether this role may cancel an order sitting at `status`. Mirrors
 * `WorkOrderStateMachine`'s CANCELLED transitions: allowed from anything not
 * already terminal and not already ready for pickup or beyond.
 */
export function mayCancel(status: WorkOrderStatus, role: WorkerRole | null): boolean {
  if (!role || isTerminal(status)) return false;
  return CANCEL_ROLES.includes(role);
}
