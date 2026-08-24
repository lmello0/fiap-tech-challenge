import { Location } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Session } from '../../core/auth/session';
import { Callout } from '../../shared/ui/callout';
import { homeFor } from '../../core/auth/landing';
import { safeReturnUrl } from '../sign-in/sign-in';

/**
 * One plate, two callers: a `pwd_change`-flagged account forced to rotate a
 * temporary password before anything else opens, and an ordinary signed-in
 * worker changing their password by choice from wherever they were.
 *
 * Which one this is is fixed for the component's lifetime — captured once,
 * not read reactively — because `Session.changePassword` flips
 * `passwordChangeRequired` off mid-submit and the copy must not swap under
 * the operator's hands while the request is still in flight.
 */
@Component({
  selector: 'app-change-password',
  imports: [Callout],
  template: `
    <div class="cpw">
      <div class="cpw__sheet plate">
        @if (forced) {
          <header class="cpw__head">
            <p class="cpw__eyebrow label">Auto Repair Shop</p>
            <h1 class="cpw__title">Set your password</h1>
            <p class="cpw__sub">This account was given a temporary password. It must be changed before the console will open.</p>
          </header>
        } @else {
          <header class="cpw__head">
            <p class="cpw__eyebrow label">Auto Repair Shop</p>
            <h1 class="cpw__title">Change your password</h1>
            <p class="cpw__sub">Every other session is signed out once this goes through — including this tab, briefly, while it signs back in.</p>
          </header>
        }

        <form class="cpw__form" (submit)="submit($event)">
          @if (forced) {
            <label class="cpw__field">
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
          }

          <label class="cpw__field">
            <span class="label">{{ forced ? 'Temporary password' : 'Current password' }}</span>
            <input
              class="input"
              type="password"
              name="current"
              autocomplete="current-password"
              required
              [value]="current()"
              (input)="current.set($any($event.target).value)"
            />
          </label>

          <label class="cpw__field">
            <span class="label">New password</span>
            <input
              class="input"
              type="password"
              name="next"
              autocomplete="new-password"
              required
              minlength="16"
              maxlength="72"
              [value]="next()"
              (input)="next.set($any($event.target).value)"
            />
            <span class="cpw__hint">16–72 characters.</span>
          </label>

          <label class="cpw__field">
            <span class="label">Confirm new password</span>
            <input
              class="input"
              type="password"
              name="confirm"
              autocomplete="new-password"
              required
              [value]="confirm()"
              (input)="confirm.set($any($event.target).value)"
            />
          </label>

          @if (mismatch()) {
            <app-callout tier="caution">The confirmation does not match the new password.</app-callout>
          }

          @if (session.error(); as message) {
            <app-callout tier="warning">{{ message }}</app-callout>
          }

          <button class="btn btn--primary" type="submit" [disabled]="session.busy() || !canSubmit()">
            {{ session.busy() ? 'Setting password…' : (forced ? 'Set password and continue' : 'Change password') }}
          </button>

          @if (!forced) {
            <button class="btn btn--quiet btn--sm" type="button" [disabled]="session.busy()" (click)="cancel()">
              Cancel
            </button>
          }
        </form>
      </div>
    </div>
  `,
  styleUrl: './change-password.scss',
})
export class ChangePassword {
  protected readonly session = inject(Session);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  /** True for the forced rotation; false for a signed-in worker's own choice. */
  protected readonly forced = this.session.passwordChangeRequired();

  protected readonly email = signal(readHandoffEmail());

  /** Where the interrupted sign-in was heading, if anywhere. Handed over with the email. */
  private readonly next2 = safeReturnUrl(readHandoffNext());
  protected readonly current = signal('');
  protected readonly next = signal('');
  protected readonly confirm = signal('');

  protected readonly mismatch = computed(() => this.confirm().length > 0 && this.next() !== this.confirm());
  protected readonly canSubmit = computed(
    () =>
      this.current().length > 0 &&
      this.next().length >= 16 &&
      !this.mismatch() &&
      (!this.forced || this.email().trim().length > 0),
  );

  protected async submit(event: Event): Promise<void> {
    event.preventDefault();
    if (!this.canSubmit()) return;
    const email = this.forced ? this.email().trim() : (this.session.user()?.email ?? '');
    const result = await this.session.changePassword(email, this.current(), this.next());
    this.current.set('');
    this.next.set('');
    this.confirm.set('');
    if (result === 'changed') {
      // The account may hold either facet or both, so where "in" is depends on
      // what `changePassword` resolved — never on this screen's assumption.
      const facet = this.session.facet();
      if (facet === null) {
        await this.router.navigateByUrl(
          this.next2 ? `/choose?next=${encodeURIComponent(this.next2)}` : '/choose',
        );
      } else {
        await this.router.navigateByUrl(this.next2 ?? homeFor(facet, this.session.role()));
      }
    } else if (result === 'session-lost') {
      await this.router.navigate(['sign-in']);
    }
    // 'failed': stay put — session.error() is already the callout above.
  }

  protected cancel(): void {
    this.location.back();
  }
}

/** The email a forced sign-in was attempted with, handed off via router state. */
function readHandoffEmail(): string {
  try {
    const state = history.state as { email?: string } | null;
    return state?.email ?? '';
  } catch {
    return '';
  }
}

/** The page the interrupted sign-in was heading for, handed off the same way. */
function readHandoffNext(): string | null {
  try {
    const state = history.state as { next?: string | null } | null;
    return state?.next ?? null;
  } catch {
    return null;
  }
}
