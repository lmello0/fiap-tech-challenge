import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Session } from '../../core/auth/session';
import { ShopStore } from '../../core/data/shop-store';
import { Callout } from '../../shared/ui/callout';
import { sectionsFor } from '../../core/nav';

/**
 * The cover of the manual.
 *
 * Deliberately the plainest plate in the console: two fields, one action, and
 * the standing note about demo mode. Nothing here decorates — the operator is
 * three seconds from the work and should not be detained.
 */
@Component({
  selector: 'app-sign-in',
  imports: [Callout],
  template: `
    <div class="signin">
      <div class="signin__sheet plate">
        <header class="signin__head">
          <p class="signin__eyebrow label">Auto Repair Shop</p>
          <h1 class="signin__title">Workshop Manual</h1>
          <p class="signin__sub">Internal staff console · REV {{ revision }}</p>
        </header>

        <form class="signin__form" (submit)="submit($event)">
          <label class="signin__field">
            <span class="label">Email</span>
            <input
              class="input"
              type="email"
              name="email"
              autocomplete="username"
              required
              [value]="email()"
              (input)="email.set($any($event.target).value)"
            />
          </label>

          <label class="signin__field">
            <span class="label">Password</span>
            <input
              class="input"
              type="password"
              name="password"
              autocomplete="current-password"
              required
              [value]="password()"
              (input)="password.set($any($event.target).value)"
            />
          </label>

          @if (session.error(); as message) {
            <app-callout tone="warning" heading="Not signed in">{{ message }}</app-callout>
          }

          <button class="btn btn--primary" type="submit" [disabled]="session.busy()">
            {{ session.busy() ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>

        <footer class="signin__foot">
          <p class="signin__note">
            This console acts on your Worker facet. An account without one is refused entry —
            every screen behind this point would be refused by the API too.
          </p>
          <button class="btn btn--quiet btn--sm" type="button" (click)="demo()">
            Walk the console in demo mode
          </button>
          <p class="signin__note signin__note--tight">
            Demo mode signs in to nothing and calls no API. It reads a synthetic
            shop so the screens can be reviewed against an empty database.
          </p>
        </footer>
      </div>
    </div>
  `,
  styleUrl: './sign-in.scss',
})
export class SignIn {
  protected readonly session = inject(Session);
  private readonly store = inject(ShopStore);
  private readonly router = inject(Router);

  protected readonly revision = '2026.08';

  protected readonly email = signal('');
  protected readonly password = signal('');

  protected async submit(event: Event): Promise<void> {
    event.preventDefault();
    if (await this.session.signIn(this.email().trim(), this.password())) {
      this.password.set('');
      await this.land();
    }
  }

  protected async demo(): Promise<void> {
    await this.session.enterDemo();
    await this.land();
  }

  /** Land on the first section this role is actually allowed to open. */
  private async land(): Promise<void> {
    void this.store;
    const first = sectionsFor(this.session.role())[0]?.path ?? 'work-orders';
    await this.router.navigate([first]);
  }
}
