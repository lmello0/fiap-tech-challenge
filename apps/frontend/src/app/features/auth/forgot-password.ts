import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/api/api-client';
import { ShopApi } from '../../core/api/shop-api';
import { Callout } from '../../shared/ui/callout';

/**
 * Asks the API to email a reset link.
 *
 * The backend answers 204 whether or not the address belongs to an account —
 * it does not say which, on purpose — so this plate shows the same "check
 * your email" state either way rather than inventing a distinction the API
 * deliberately withholds.
 */
@Component({
  selector: 'app-forgot-password',
  imports: [Callout, RouterLink],
  template: `
    <div class="fpw">
      <div class="fpw__sheet plate">
        @if (sent()) {
          <header class="fpw__head">
            <p class="fpw__eyebrow label">Auto Repair Shop</p>
            <h1 class="fpw__title">Check your email</h1>
            <p class="fpw__sub">
              If {{ submittedEmail() }} belongs to an account, a reset link is on its way. It expires
              after a short while, so open it soon.
            </p>
          </header>
          <a class="btn btn--quiet" routerLink="/sign-in">Back to sign in</a>
        } @else {
          <header class="fpw__head">
            <p class="fpw__eyebrow label">Auto Repair Shop</p>
            <h1 class="fpw__title">Reset your password</h1>
            <p class="fpw__sub">Enter the email on your account and we'll send a link to choose a new one.</p>
          </header>

          <form class="fpw__form" (submit)="submit($event)">
            <label class="fpw__field">
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

            @if (error(); as message) {
              <app-callout tier="warning">{{ message }}</app-callout>
            }

            <button class="btn btn--primary" type="submit" [disabled]="busy()">
              {{ busy() ? 'Sending…' : 'Send reset link' }}
            </button>
            <a class="fpw__link" routerLink="/sign-in">Back to sign in</a>
          </form>
        }
      </div>
    </div>
  `,
  styleUrl: './forgot-password.scss',
})
export class ForgotPassword {
  private readonly api = inject(ShopApi);

  protected readonly email = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly sent = signal(false);
  protected readonly submittedEmail = signal('');

  protected async submit(event: Event): Promise<void> {
    event.preventDefault();
    const email = this.email().trim();
    if (!email) return;
    this.busy.set(true);
    this.error.set(null);
    try {
      await this.api.requestPasswordReset(email);
      this.submittedEmail.set(email);
      this.sent.set(true);
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'Could not send the reset link.');
    } finally {
      this.busy.set(false);
    }
  }
}
