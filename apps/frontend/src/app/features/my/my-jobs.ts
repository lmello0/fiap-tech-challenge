import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/api/api-client';
import { CustomerStore } from '../../core/customer/customer-store';
import { customerStepFor } from '../../core/domain/customer-procedure';
import { Callout } from '../../shared/ui/callout';
import { StatusMark } from '../../shared/ui/status-mark';

/**
 * Every job this device knows about, and the way to add one it does not.
 *
 * The honesty this page owes the reader is stated on the page: the shop's API
 * has no "list my work orders" call, so this register is what this browser has
 * been shown, not a statement about the account. That is the difference
 * between a list that is quietly wrong and one that says what it is — and the
 * reference blank underneath turns the limitation into something the reader
 * can act on rather than a shrug.
 */
@Component({
  selector: 'app-my-jobs',
  imports: [Callout, RouterLink, StatusMark],
  templateUrl: './my-jobs.html',
  styleUrls: ['./garage.scss', './my-jobs.scss'],
})
export class MyJobs {
  protected readonly store = inject(CustomerStore);
  protected readonly step = customerStepFor;

  protected readonly reference = signal('');
  protected readonly adding = signal(false);
  protected readonly addError = signal<string | null>(null);
  protected readonly added = signal<string | null>(null);

  protected async track(event: Event): Promise<void> {
    event.preventDefault();
    const value = this.reference().trim();
    if (!value || this.adding()) return;

    this.adding.set(true);
    this.addError.set(null);
    this.added.set(null);
    try {
      const job = await this.store.trackJob(value);
      if (job) {
        this.reference.set('');
        this.added.set(job.orderCode);
      }
    } catch (error) {
      // 404 covers both "no such job" and "not yours" — the API refuses to
      // distinguish them, so neither does this message.
      this.addError.set(
        error instanceof ApiError && error.isNotFound
          ? 'No job on your account has that reference. Check it against the email the shop sent.'
          : error instanceof ApiError
            ? error.message
            : 'That reference could not be read.',
      );
    } finally {
      this.adding.set(false);
    }
  }

  protected money(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }
}
