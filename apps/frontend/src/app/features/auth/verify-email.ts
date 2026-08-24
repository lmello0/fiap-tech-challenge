import { Component, OnInit, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { ApiError } from '../../core/api/api-client';
import { ShopApi } from '../../core/api/shop-api';
import { Icon } from '../../shared/ui/icon';

type Stage = 'checking' | 'done' | 'invalid';

/**
 * Where `/verify-email?token=…` from the email lands. Confirms itself on
 * arrival — there is nothing for the operator to fill in, so the plate is a
 * status report rather than a form.
 */
@Component({
  selector: 'app-verify-email',
  imports: [Icon, RouterLink],
  template: `
    <div class="vem">
      <div class="vem__sheet plate">
        @switch (stage()) {
          @case ('checking') {
            <p class="vem__eyebrow label">Auto Repair Shop</p>
            <h1 class="vem__title">Confirming your email…</h1>
          }
          @case ('done') {
            <app-icon name="check" class="vem__mark vem__mark--ok" [size]="28" />
            <p class="vem__eyebrow label">Auto Repair Shop</p>
            <h1 class="vem__title">Email confirmed</h1>
            <p class="vem__sub">Your address is verified. You can sign in now.</p>
            <a class="btn btn--primary" routerLink="/sign-in">Go to sign in</a>
          }
          @case ('invalid') {
            <app-icon name="alert" class="vem__mark vem__mark--warn" [size]="28" />
            <p class="vem__eyebrow label">Auto Repair Shop</p>
            <h1 class="vem__title">This link no longer works</h1>
            <p class="vem__sub">{{ error() }} Ask for a new confirmation email from the sign-in page.</p>
            <a class="btn btn--primary" routerLink="/sign-in">Back to sign in</a>
          }
        }
      </div>
    </div>
  `,
  styleUrl: './verify-email.scss',
})
export class VerifyEmail implements OnInit {
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
      await this.api.confirmEmailVerification(token);
      this.stage.set('done');
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'This link is invalid or has expired.');
      this.stage.set('invalid');
    }
  }
}
