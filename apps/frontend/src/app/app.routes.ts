import { inject } from '@angular/core';
import { Router, type CanMatchFn, type Routes, type UrlSegment } from '@angular/router';
import { Session } from './core/auth/session';
import { homeFor } from './core/auth/landing';
import { sectionsFor } from './core/nav';
import type { WorkerRole } from './core/domain/enums';

/** `['my','jobs','abc']` → `/my/jobs/abc`, for a `?next=` a guard can hand back. */
function pathOf(segments: UrlSegment[]): string {
  return `/${segments.map((s) => s.path).join('/')}`;
}

/**
 * Send an unauthenticated caller to sign in, remembering where they were going.
 *
 * The remembered path is the whole point: a budget link in an email lands on a
 * job, and dropping someone on a front page after they sign in makes them go
 * looking for what they clicked.
 */
function toSignIn(router: Router, segments: UrlSegment[]) {
  return router.createUrlTree(['sign-in'], { queryParams: { next: pathOf(segments) } });
}

/** Behind the staff cover: an active Worker facet, or demo mode. */
const inWorkshop: CanMatchFn = (_route, segments) => {
  const session = inject(Session);
  const router = inject(Router);
  if (session.passwordChangeRequired()) return router.createUrlTree(['change-password']);
  if (session.inWorkshop()) return true;

  // Signed in, just not on this side of the counter. A customer who follows a
  // staff link is not refused — they are sent where they can actually be.
  if (session.signedIn()) {
    return session.hasWorkerFacet()
      ? router.createUrlTree(['choose'], { queryParams: { next: pathOf(segments) } })
      : router.createUrlTree(['my']);
  }
  return toSignIn(router, segments);
};

/** Behind the owner's cover: an active Customer facet. */
const inGarage: CanMatchFn = (_route, segments) => {
  const session = inject(Session);
  const router = inject(Router);
  if (session.passwordChangeRequired()) return router.createUrlTree(['change-password']);
  if (session.inGarage()) return true;

  if (session.signedIn()) {
    // Demo mode has no customer facet at all — it signs in to nothing.
    if (!session.hasCustomerFacet()) return router.createUrlTree(['work-orders']);
    return router.createUrlTree(['choose'], { queryParams: { next: pathOf(segments) } });
  }
  return toSignIn(router, segments);
};

/** The picker exists only for an account that genuinely has a choice to make. */
const canChoose: CanMatchFn = () => {
  const session = inject(Session);
  const router = inject(Router);
  if (session.hasBothFacets()) return true;
  if (!session.signedIn()) return router.createUrlTree(['sign-in']);
  const facet = session.facet();
  return router.createUrlTree([homeFor(facet ?? 'customer', session.role())]);
};

/**
 * `/change-password` serves three callers: a `pwd_change`-flagged session with
 * a forced rotation, a signed-in worker changing it by choice, and a customer
 * doing the same from their own details. Demo mode signs in to nothing, so
 * there is no real password behind it.
 */
const canChangePassword: CanMatchFn = () => {
  const session = inject(Session);
  const router = inject(Router);
  const allowed = session.passwordChangeRequired() || (session.signedIn() && !session.isDemo());
  return allowed ? true : router.createUrlTree(['sign-in']);
};

/**
 * Mirrors the backend's `@PreAuthorize` on each staff section. A role that
 * reaches a route it cannot use is sent to the first section it can — never
 * shown an empty screen it has no rights to fill.
 */
function allow(...roles: WorkerRole[]): CanMatchFn {
  return () => {
    const session = inject(Session);
    const router = inject(Router);
    if (session.hasAnyRole(...roles)) return true;
    const fallback = sectionsFor(session.role())[0]?.path ?? 'work-orders';
    return router.createUrlTree([fallback]);
  };
}

export const routes: Routes = [
  /* --- the public front ------------------------------------------------- */

  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/landing/landing').then((m) => m.Landing),
  },
  {
    path: 'sign-in',
    loadComponent: () => import('./features/sign-in/sign-in').then((m) => m.SignIn),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/register/register').then((m) => m.Register),
  },
  {
    path: 'choose',
    canMatch: [canChoose],
    loadComponent: () =>
      import('./features/choose-facet/choose-facet').then((m) => m.ChooseFacet),
  },

  /* --- account plumbing, open to both facets ----------------------------- */

  {
    path: 'change-password',
    canMatch: [canChangePassword],
    loadComponent: () => import('./features/auth/change-password').then((m) => m.ChangePassword),
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/auth/reset-password').then((m) => m.ResetPassword),
  },
  {
    path: 'verify-email',
    loadComponent: () => import('./features/auth/verify-email').then((m) => m.VerifyEmail),
  },
  {
    path: 'confirm-email-change',
    loadComponent: () =>
      import('./features/auth/confirm-email-change').then((m) => m.ConfirmEmailChange),
  },

  /* --- volume two: the owner's manual ------------------------------------ */

  {
    path: 'my',
    canMatch: [inGarage],
    loadComponent: () => import('./features/my/garage-shell').then((m) => m.GarageShell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./features/my/front-page').then((m) => m.GarageFrontPage),
      },
      {
        path: 'vehicles',
        loadComponent: () => import('./features/my/my-vehicles').then((m) => m.MyVehicles),
      },
      {
        path: 'jobs',
        loadComponent: () => import('./features/my/my-jobs').then((m) => m.MyJobs),
      },
      {
        path: 'jobs/:id',
        loadComponent: () => import('./features/my/my-job').then((m) => m.MyJob),
      },
      {
        path: 'booking',
        loadComponent: () => import('./features/my/my-booking').then((m) => m.MyBooking),
      },
      {
        path: 'details',
        loadComponent: () => import('./features/my/my-details').then((m) => m.MyDetails),
      },
      { path: '**', redirectTo: '' },
    ],
  },

  /* --- volume one: the shop manual --------------------------------------- */

  {
    path: 'work-orders',
    canMatch: [inWorkshop, allow('ATTENDANT', 'MECHANIC', 'MANAGER')],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/work-orders/board').then((m) => m.WorkOrderBoard),
      },
      {
        path: ':id',
        loadComponent: () => import('./features/work-orders/detail').then((m) => m.WorkOrderDetail),
      },
    ],
  },
  {
    path: 'schedule',
    canMatch: [inWorkshop, allow('ATTENDANT', 'MANAGER')],
    loadComponent: () => import('./features/schedule/schedule').then((m) => m.Schedule),
  },
  {
    path: 'inventory',
    canMatch: [inWorkshop, allow('MECHANIC', 'STOCKIST', 'MANAGER')],
    loadComponent: () => import('./features/inventory/inventory').then((m) => m.Inventory),
  },
  {
    path: 'customers',
    canMatch: [inWorkshop, allow('ATTENDANT', 'MANAGER')],
    loadComponent: () => import('./features/records/customers').then((m) => m.Customers),
  },
  {
    path: 'vehicles',
    canMatch: [inWorkshop, allow('ATTENDANT', 'MANAGER')],
    loadComponent: () => import('./features/records/vehicles').then((m) => m.Vehicles),
  },
  {
    path: 'workers',
    canMatch: [inWorkshop, allow('MANAGER')],
    loadComponent: () => import('./features/records/workers').then((m) => m.Workers),
  },

  // Anything else is the front door, not a staff screen: this app is now
  // public, and a stray URL should land a stranger somewhere that makes sense.
  { path: '**', redirectTo: '' },
];
