import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/api/api-client';
import { ShopApi } from '../../core/api/shop-api';
import { Session, type SignInOutcome } from '../../core/auth/session';
import { landingFor } from '../../core/auth/landing';
import { Callout } from '../../shared/ui/callout';

/**
 * One door, both volumes.
 *
 * This used to be the staff cover and it refused anyone without a Worker
 * facet outright. It cannot any more: the same account may hold a Customer
 * facet, a Worker facet, or both, and which one a person is here for is not
 * knowable from an email address. So this screen authenticates and then hands
 * the outcome to `landingFor` — one usable facet goes straight to its volume,
 * two stop at the picker, none is refused with the reason printed.
 *
 * `?next=` is honoured on the way through. A guard that turned someone away
 * from a deep link records where they were going; landing them on a dashboard
 * instead and making them navigate back is the small rudeness this avoids.
 * Only same-origin relative paths are followed — an absolute URL in a query
 * parameter is an open redirect, not a convenience.
 */
@Component({
  selector: 'app-sign-in',
  imports: [Callout, RouterLink],
  templateUrl: './sign-in.html',
  styleUrl: './sign-in.scss',
})
export class SignIn {
  protected readonly session = inject(Session);
  private readonly api = inject(ShopApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly revision = '2026.08';

  protected readonly email = signal('');
  protected readonly password = signal('');

  /** Set when the API refused the login purely because the address is unconfirmed. */
  protected readonly unverified = signal<string | null>(null);
  protected readonly resending = signal(false);
  protected readonly resent = signal(false);
  protected readonly resendError = signal<string | null>(null);

  /** Where the guard wanted them, if a guard sent them here. */
  private readonly next = safeReturnUrl(this.route.snapshot.queryParamMap.get('next'));

  protected readonly returningTo = this.next;

  protected async submit(event: Event): Promise<void> {
    event.preventDefault();
    const email = this.email().trim();
    this.unverified.set(null);
    this.resent.set(false);
    const outcome = await this.session.signIn(email, this.password());
    this.password.set('');

    if (outcome.kind === 'email-unverified') {
      this.unverified.set(outcome.email);
      return;
    }
    if (outcome.kind === 'password-change') {
      // `resume` never ran, so the account — and its email — is not known to
      // the session yet. Hand it over via navigation state rather than asking
      // for what was just typed.
      await this.router.navigate(['change-password'], { state: { email, next: this.next } });
      return;
    }
    await this.land(outcome);
  }

  /**
   * The only useful thing to offer someone the API is refusing on this ground.
   * The endpoint answers 204 whether or not the address is an unverified
   * account, so this screen says the same thing either way.
   */
  protected async resend(): Promise<void> {
    const email = this.unverified();
    if (!email || this.resending()) return;
    this.resending.set(true);
    this.resendError.set(null);
    try {
      await this.api.resendEmailVerification(email);
      this.resent.set(true);
    } catch (error) {
      this.resendError.set(
        error instanceof ApiError ? error.message : 'The link could not be sent.',
      );
    } finally {
      this.resending.set(false);
    }
  }

  protected async demo(): Promise<void> {
    await this.session.enterDemo();
    await this.router.navigateByUrl('/work-orders');
  }

  private async land(outcome: SignInOutcome): Promise<void> {
    const target = landingFor(outcome, this.next, this.session.role());
    if (target) await this.router.navigateByUrl(target);
  }
}

/**
 * A return path is followed only when it is unmistakably one of ours.
 *
 * Anything that could name another origin — a scheme, a protocol-relative
 * `//host`, a backslash some parsers fold to a slash — is dropped and the
 * caller lands on their facet's own home instead.
 */
export function safeReturnUrl(value: string | null): string | null {
  if (!value) return null;
  if (!value.startsWith('/')) return null;
  if (value.startsWith('//') || value.startsWith('/\\')) return null;
  if (value.includes('://')) return null;
  return value;
}
