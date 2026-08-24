import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { ApiError } from '../../core/api/api-client';
import { ShopApi } from '../../core/api/shop-api';
import { Callout } from '../../shared/ui/callout';

type Stage = 'form' | 'done' | 'invalid';

/**
 * Where `/reset-password?token=…` from the email lands.
 *
 * There is no separate validate-only endpoint — the confirm call is the
 * validation, so a dead token is only discovered on submit, not on arrival.
 * A missing token skips straight to the invalid state: there is nothing to
 * submit.
 */
@Component({
  selector: 'app-reset-password',
  imports: [Callout, RouterLink],
  template: `
    <div class="rpw">
      <div class="rpw__sheet plate">
        @switch (stage()) {
          @case ('invalid') {
            <header class="rpw__head">
              <p class="rpw__eyebrow label">Auto Repair Shop</p>
              <h1 class="rpw__title">This link no longer works</h1>
              <p class="rpw__sub">
                @if (error(); as message) {
                  {{ message }}
                } @else {
                  This reset link is missing its token.
                }
                Links expire and can only be used once — request a new one below.
              </p>
            </header>
            <a class="btn btn--primary" routerLink="/forgot-password">Request a new link</a>
          }
          @case ('done') {
            <header class="rpw__head">
              <p class="rpw__eyebrow label">Auto Repair Shop</p>
              <h1 class="rpw__title">Password changed</h1>
              <p class="rpw__sub">Sign in with your new password. Every other session was signed out.</p>
            </header>
            <a class="btn btn--primary" routerLink="/sign-in">Go to sign in</a>
          }
          @default {
            <header class="rpw__head">
              <p class="rpw__eyebrow label">Auto Repair Shop</p>
              <h1 class="rpw__title">Choose a new password</h1>
            </header>

            <form class="rpw__form" (submit)="submit($event)">
              <label class="rpw__field">
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
                <span class="rpw__hint">16–72 characters.</span>
              </label>

              <label class="rpw__field">
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

              @if (error(); as message) {
                <app-callout tier="warning">{{ message }}</app-callout>
              }

              <button class="btn btn--primary" type="submit" [disabled]="busy() || !canSubmit()">
                {{ busy() ? 'Setting password…' : 'Set new password' }}
              </button>
            </form>
          }
        }
      </div>
    </div>
  `,
  styleUrl: './reset-password.scss',
})
export class ResetPassword {
  private readonly api = inject(ShopApi);
  private readonly route = inject(ActivatedRoute);

  private readonly token = toSignal(this.route.queryParamMap.pipe(map((params) => params.get('token'))), {
    initialValue: this.route.snapshot.queryParamMap.get('token'),
  });

  protected readonly next = signal('');
  protected readonly confirm = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly stage = signal<Stage>('form');

  protected readonly mismatch = computed(() => this.confirm().length > 0 && this.next() !== this.confirm());
  protected readonly canSubmit = computed(() => this.next().length >= 16 && !this.mismatch());

  constructor() {
    if (!this.token()) this.stage.set('invalid');
  }

  protected async submit(event: Event): Promise<void> {
    event.preventDefault();
    const token = this.token();
    if (!token || !this.canSubmit()) return;
    this.busy.set(true);
    this.error.set(null);
    try {
      await this.api.confirmPasswordReset(token, this.next());
      this.next.set('');
      this.confirm.set('');
      this.stage.set('done');
    } catch (error) {
      this.error.set(
        error instanceof ApiError ? error.message : 'Could not reset the password.',
      );
      if (error instanceof ApiError && error.isAuth) this.stage.set('invalid');
    } finally {
      this.busy.set(false);
    }
  }
}
