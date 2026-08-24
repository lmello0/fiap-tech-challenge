import type { Facet, SignInOutcome } from './session';
import { sectionsFor } from '../nav';
import type { WorkerRole } from '../domain/enums';

/**
 * Where a resolved sign-in belongs.
 *
 * Three screens need this answer — sign-in, registration and the forced
 * password change — and all three used to work it out for themselves, which
 * is exactly how one of them ends up sending a customer to `/work-orders`.
 *
 * The requested page wins when there is one and the facet can actually open
 * it: a link into the staff console is not a destination for a customer, and
 * following it would only trade this redirect for a guard's redirect one
 * navigation later.
 */
export function landingFor(
  outcome: SignInOutcome,
  next: string | null,
  role: WorkerRole | null = null,
): string | null {
  switch (outcome.kind) {
    case 'facet':
      return next && reachableBy(outcome.facet, next) ? next : homeFor(outcome.facet, role);
    case 'choose':
      // The picker keeps the destination so the choice still lands on it.
      return next ? `/choose?next=${encodeURIComponent(next)}` : '/choose';
    default:
      // Refused, or confined to the password screen. The screen that asked
      // prints the reason; navigating away would take it off the page.
      return null;
  }
}

/** The customer volume opens on its own front page; the staff one on a section. */
export function homeFor(facet: Facet, role: WorkerRole | null = null): string {
  if (facet === 'customer') return '/my';
  return `/${sectionsFor(role)[0]?.path ?? 'work-orders'}`;
}

/**
 * Whether a facet could open a path at all.
 *
 * Deliberately coarse — it asks which volume a path belongs to, not whether
 * this particular role may open that particular section. The route guards own
 * the second question, and duplicating them here would be a second copy of the
 * backend's `@PreAuthorize` to keep in step.
 */
function reachableBy(facet: Facet, path: string): boolean {
  const inGarage = path === '/my' || path.startsWith('/my/');
  return facet === 'customer' ? inGarage : !inGarage;
}
