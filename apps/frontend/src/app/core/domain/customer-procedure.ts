import { CANCELLED_STEP, LIFECYCLE, REFUSED_STEP, type LifecycleStep } from './lifecycle';
import type { WorkOrderStatus } from './enums';

/**
 * The same procedure, read from the customer's side of the counter.
 *
 * `lifecycle.ts` is the spine and stays the spine: the steps, their order and
 * their numbers all come from it, so a backend rule that moves there moves
 * here too. What this file adds is the *second reading* — the staff manual's
 * preconditions are written for the person performing the step ("Every
 * reserved part is on the shelf"), which is the wrong sentence to print for
 * the person waiting in the car park.
 *
 * Two things are marked rather than described, because they are the only two
 * points where the shop is waiting on the customer and not the other way
 * round: the budget decision, and choosing a pickup slot.
 */
export interface CustomerStep {
  readonly n: number;
  readonly status: WorkOrderStatus;
  /** The staff manual's own title — the same words the shop uses on the phone. */
  readonly title: string;
  /** What it means for the person whose car it is. */
  readonly line: string;
  /** True where nothing moves until the customer does something. */
  readonly waitsOnYou: boolean;
}

const LINES: Record<WorkOrderStatus, { line: string; waitsOnYou: boolean }> = {
  RECEIVED: {
    line: 'Your car is with us and the complaint you described is on the order.',
    waitsOnYou: false,
  },
  WAITING_DIAGNOSTICS: {
    line: 'Queued for a mechanic to look at it. Nothing has been touched yet.',
    waitsOnYou: false,
  },
  IN_DIAGNOSTICS: {
    line: 'A mechanic is inspecting it. This is the diagnosis, not the repair.',
    waitsOnYou: false,
  },
  BUDGET_IN_DRAFT: {
    line: 'They are costing the work. You have not been asked for anything yet.',
    waitsOnYou: false,
  },
  WAITING_APPROVAL: {
    line: 'The price is with you. The lines are frozen — nothing can be added to what you are looking at, and no work starts until you say so.',
    waitsOnYou: true,
  },
  APPROVED: {
    line: 'You approved it. The shop is holding the parts for your job.',
    waitsOnYou: false,
  },
  REFUSED: {
    line: 'You refused the price and the order ends there. It cannot be requoted — a new job would start from the beginning.',
    waitsOnYou: false,
  },
  IN_PROGRESS: {
    line: 'The work you approved is being done.',
    waitsOnYou: false,
  },
  FINISHED: {
    line: 'Every line you approved is finished.',
    waitsOnYou: false,
  },
  WAITING_PICKUP: {
    line: 'Ready to collect. Choose a pickup slot and the counter will have it waiting.',
    waitsOnYou: true,
  },
  DELIVERED: {
    line: 'You collected it. The job is closed.',
    waitsOnYou: false,
  },
  CANCELLED: {
    line: 'The shop cancelled this order. It cannot be reopened — call the counter if you still need the work done.',
    waitsOnYou: false,
  },
};

function toCustomerStep(step: LifecycleStep): CustomerStep {
  return { n: step.n, status: step.status, title: step.title, ...LINES[step.status] };
}

/** The main line, ten steps, in order. */
export const CUSTOMER_PROCEDURE: readonly CustomerStep[] = LIFECYCLE.map(toCustomerStep);

/** The branches off it. Both terminal. */
export const CUSTOMER_REFUSED: CustomerStep = toCustomerStep(REFUSED_STEP);
export const CUSTOMER_CANCELLED: CustomerStep = toCustomerStep(CANCELLED_STEP);

const BY_STATUS = new Map<WorkOrderStatus, CustomerStep>([
  ...CUSTOMER_PROCEDURE.map((s) => [s.status, s] as const),
  ['REFUSED', CUSTOMER_REFUSED] as const,
  ['CANCELLED', CUSTOMER_CANCELLED] as const,
]);

export function customerStepFor(status: WorkOrderStatus): CustomerStep {
  return BY_STATUS.get(status)!;
}
