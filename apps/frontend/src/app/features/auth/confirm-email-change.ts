import { Component, OnInit, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { ApiError } from '../../core/api/api-client';
import { ShopApi } from '../../core/api/shop-api';
import { Icon } from '../../shared/ui/icon';

type Stage = 'checking' | 'done' | 'invalid';

/**
 * Where `/confirm-email-change?token=…` from the email lands. Confirms
 * itself on arrival, same shape as {@link VerifyEmail}. The API revokes every
 * session on success, so the only place left to go is back to sign-in.
 */
@Component({
  selector: 'app-confirm-email-change',
  imports: [Icon, RouterLink],
  template: `
    <div class="cec">
      <div class="cec__sheet plate">
        @switch (stage()) {
          @case ('checking') {
            <p class="cec__eyebrow label">Auto Repair Shop</p>
            <h1 class="cec__title">Confirming your new address…</h1>
          }
          @case ('done') {
            <app-icon name="check" class="cec__mark cec__mark--ok" [size]="28" />
            <p class="cec__eyebrow label">Auto Repair Shop</p>
            <h1 class="cec__title">Email address changed</h1>
            <p class="cec__sub">Every session was signed out for safety. Sign in again with your new address.</p>
            <a class="btn btn--primary" routerLink="/sign-in">Go to sign in</a>
          }
          @case ('invalid') {
            <app-icon name="alert" class="cec__mark cec__mark--warn" [size]="28" />
            <p class="cec__eyebrow label">Auto Repair Shop</p>
            <h1 class="cec__title">This link no longer works</h1>
            <p class="cec__sub">{{ error() }} Your email address was not changed. Request the change again from your account.</p>
            <a class="btn btn--primary" routerLink="/sign-in">Back to sign in</a>
          }
        }
      </div>
    </div>
  `,
  styleUrl: './confirm-email-change.scss',
})
export class ConfirmEmailChange implements OnInit {
  private readonly api = inject(ShopApi);
  private readonly route = inject(ActivatedRoute);

  private readonly token = toSignal(this.route.queryParamMap.pipe(map((params) => params.get('token'))), {
    initialValue: this.route.snapshot.queryParamMap.get('token'),
  });

  protected readonly stage = signal<Stage>('checking');
  protected readonly error = signal('This link is missing its token.');

  async ngOnInit(): Promise<void> {
    const token = this.token();
    if (!token) {
      this.stage.set('invalid');
      return;
    }
    try {
      await this.api.confirmEmailChange(token);
      this.stage.set('done');
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'This link is invalid or has expired.');
      this.stage.set('invalid');
    }
  }
}
