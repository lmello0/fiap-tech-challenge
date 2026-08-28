import { Component, computed, signal, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiError } from '../../core/api/api-client';
import { ShopApi } from '../../core/api/shop-api';
import { toBudget } from '../../core/data/mappers';
import type { Budget } from '../../core/domain/models';
import { Callout } from '../../shared/ui/callout';
import { Icon } from '../../shared/ui/icon';

type Pending = 'approve' | 'refuse' | null;

/**
 * `/budgets/decide?token=…` — the link `WorkOrderEmails` mails when a Budget
 * is ready, landed for the first time (ADR 0021).
 *
 * Same shape as the other three guest links: the token in the URL is the
 * whole credential, nothing here is guarded, and nothing destructive fires on
 * arrival — the page reads the Budget and waits. It answers a customer
 * whether or not they have signed in anywhere, because the backend never
 * asked which: `POST /budgets/decision/*` takes the token, not a JWT.
 *
 * `BudgetInfo` carries no order code or vehicle — the token identifies a
 * Budget, not a work order — so this page shows exactly that: the price and
 * the decision, nothing borrowed from a screen this link was never given.
 */
@Component({
  selector: 'app-budget-decide',
  imports: [Callout, Icon, RouterLink],
  templateUrl: './budget-decide.html',
  styleUrls: ['./guest.scss', './budget-decide.scss'],
})
export class BudgetDecide {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ShopApi);

  private readonly token = this.route.snapshot.queryParamMap.get('token');

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly budget = signal<Budget | null>(null);

  protected readonly pending = signal<Pending>(null);
  protected readonly confirming = signal<Pending>(null);
  protected readonly reason = signal('');
  protected readonly actionError = signal<string | null>(null);

  protected readonly decidable = computed(() => this.budget()?.status === 'SENT');

  constructor() {
    void this.open();
  }

  private async open(): Promise<void> {
    if (!this.token) {
      this.loadError.set('This link is missing its token.');
      this.loading.set(false);
      return;
    }
    try {
      this.budget.set(toBudget(await this.api.viewBudgetByToken(this.token)));
    } catch (error) {
      // Both "no such token" and "signature doesn't match" arrive here. The
      // API refuses to tell them apart, so neither does this message.
      this.loadError.set(
        error instanceof ApiError ? error.message : 'This link is invalid or has expired.',
      );
    } finally {
      this.loading.set(false);
    }
  }

  protected async approve(): Promise<void> {
    if (!this.token || this.pending()) return;
    this.pending.set('approve');
    this.actionError.set(null);
    try {
      this.budget.set(toBudget(await this.api.approveBudgetByToken(this.token)));
      this.confirming.set(null);
    } catch (error) {
      this.actionError.set(
        error instanceof ApiError ? error.message : 'The approval was not recorded.',
      );
    } finally {
      this.pending.set(null);
    }
  }

  protected async refuse(): Promise<void> {
    if (!this.token || this.pending()) return;
    this.pending.set('refuse');
    this.actionError.set(null);
    try {
      this.budget.set(
        toBudget(await this.api.refuseBudgetByToken(this.token, this.reason().trim() || null)),
      );
      this.confirming.set(null);
      this.reason.set('');
    } catch (error) {
      this.actionError.set(
        error instanceof ApiError ? error.message : 'The refusal was not recorded.',
      );
    } finally {
      this.pending.set(null);
    }
  }

  protected money(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  protected when(instant: string): string {
    return new Date(instant).toLocaleString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
