import { inject } from '@angular/core';
import { Router, type CanMatchFn, type Routes } from '@angular/router';
import { Session } from './core/auth/session';
import { sectionsFor } from './core/nav';
import type { WorkerRole } from './core/domain/enums';

/** Nothing behind the cover is reachable without a Worker facet. */
const signedIn: CanMatchFn = () => {
  const session = inject(Session);
  const router = inject(Router);
  return session.signedIn() ? true : router.createUrlTree(['sign-in']);
};

/**
 * Mirrors the backend's `@PreAuthorize` on each section. A role that reaches a
 * route it cannot use is sent to the first section it can — never shown an
 * empty screen it has no rights to fill.
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
  { path: '', pathMatch: 'full', redirectTo: 'work-orders' },

  {
    path: 'sign-in',
    loadComponent: () => import('./features/sign-in/sign-in').then((m) => m.SignIn),
  },

  {
    path: 'work-orders',
    canMatch: [signedIn, allow('ATTENDANT', 'MECHANIC', 'MANAGER')],
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
    canMatch: [signedIn, allow('ATTENDANT', 'MANAGER')],
    loadComponent: () => import('./features/schedule/schedule').then((m) => m.Schedule),
  },
  {
    path: 'inventory',
    canMatch: [signedIn, allow('MECHANIC', 'STOCKIST', 'MANAGER')],
    loadComponent: () => import('./features/inventory/inventory').then((m) => m.Inventory),
  },
  {
    path: 'customers',
    canMatch: [signedIn, allow('ATTENDANT', 'MANAGER')],
    loadComponent: () => import('./features/records/customers').then((m) => m.Customers),
  },
  {
    path: 'vehicles',
    canMatch: [signedIn, allow('ATTENDANT', 'MANAGER')],
    loadComponent: () => import('./features/records/vehicles').then((m) => m.Vehicles),
  },
  {
    path: 'workers',
    canMatch: [signedIn, allow('MANAGER')],
    loadComponent: () => import('./features/records/workers').then((m) => m.Workers),
  },

  { path: '**', redirectTo: 'work-orders' },
];
