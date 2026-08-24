import type { WorkerRole } from './domain/enums';

/**
 * The manual's section index.
 *
 * `roles` mirrors the backend's `@PreAuthorize` on each section's controllers
 * exactly, so a tab is only printed for someone the API would actually serve.
 * A Mechanic genuinely has no Customers or Vehicles section — that is the
 * backend's rule, not a simplification.
 */
export interface Section {
  readonly path: string;
  readonly title: string;
  readonly sub: string;
  /** The manual's own section number — matches the plate numbering (4.1, 5.1 …). */
  readonly n: number;
  /** The section's own ink, used on its tab and its plate rule. */
  readonly ink: string;
  readonly roles: readonly WorkerRole[];
}

export const SECTIONS: readonly Section[] = [
  {
    path: 'work-orders',
    title: 'Work Orders',
    sub: 'Intake to delivery',
    n: 4,
    ink: 'var(--sec-work-orders)',
    roles: ['ATTENDANT', 'MECHANIC', 'MANAGER'],
  },
  {
    path: 'schedule',
    title: 'Schedule',
    sub: 'Appointments and calendar',
    n: 5,
    ink: 'var(--sec-schedule)',
    roles: ['ATTENDANT', 'MANAGER'],
  },
  {
    path: 'inventory',
    title: 'Inventory',
    sub: 'Parts, stock and purchasing',
    n: 6,
    ink: 'var(--sec-inventory)',
    roles: ['MECHANIC', 'STOCKIST', 'MANAGER'],
  },
  {
    path: 'customers',
    title: 'Customers',
    sub: 'Accounts and facets',
    n: 7,
    ink: 'var(--sec-customers)',
    roles: ['ATTENDANT', 'MANAGER'],
  },
  {
    path: 'vehicles',
    title: 'Vehicles',
    sub: 'Fleet on record',
    n: 8,
    ink: 'var(--sec-vehicles)',
    roles: ['ATTENDANT', 'MANAGER'],
  },
  {
    path: 'workers',
    title: 'Workers',
    sub: 'Roster and roles',
    n: 9,
    ink: 'var(--sec-workers)',
    roles: ['MANAGER'],
  },
];

export function sectionsFor(role: WorkerRole | null): readonly Section[] {
  if (!role) return [];
  return SECTIONS.filter((s) => s.roles.includes(role));
}

/**
 * The Owner's Manual's own index.
 *
 * A second volume, numbered from 1 again: it is a different book, not a
 * restricted view of the staff console. There is no role gating here — a
 * CUSTOMER principal is a single kind of caller, and every section below maps
 * to an endpoint the API serves them.
 */
export const CUSTOMER_SECTIONS: readonly Section[] = [
  {
    path: 'my/vehicles',
    title: 'Your vehicles',
    sub: 'What we have on record',
    n: 1,
    ink: 'var(--sec-vehicles)',
    roles: [],
  },
  {
    path: 'my/jobs',
    title: 'Your jobs',
    sub: 'Where each repair stands',
    n: 2,
    ink: 'var(--sec-work-orders)',
    roles: [],
  },
  {
    path: 'my/booking',
    title: 'Book a drop-off',
    sub: 'Choose a slot',
    n: 3,
    ink: 'var(--sec-schedule)',
    roles: [],
  },
  {
    path: 'my/details',
    title: 'Your details',
    sub: 'Name, phone and account',
    n: 4,
    ink: 'var(--sec-customers)',
    roles: [],
  },
];
