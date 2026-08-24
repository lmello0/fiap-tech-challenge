import { Component, computed, effect, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs';
import { ApiError } from '../../core/api/api-client';
import { ShopApi } from '../../core/api/shop-api';
import { CustomerStore } from '../../core/customer/customer-store';
import { CUSTOMER_PROCEDURE, customerStepFor } from '../../core/domain/customer-procedure';
import { toHistoryEntry } from '../../core/data/mappers';
import type { HistoryEntry } from '../../core/domain/models';
import { Callout } from '../../shared/ui/callout';
import { StatusMark } from '../../shared/ui/status-mark';

type Pending = 'approve' | 'refuse' | null;

/**
 * One job, and the decision the whole lifecycle waits on.
 *
 * Two things here are load-bearing and neither is decoration.
 *
 * **The step rail.** The same device as the staff console's, printed with the
 * customer's reading of each step. It is the honest answer to "where is my
 * car" — a position in a numbered procedure, with what happens next stated,
 * rather than a percentage bar nobody can audit.
 *
 * **The budget.** A sent budget is frozen: the shop cannot add to it after the
 * fact, and refusing it ends the work order permanently — there is no
 * requoting in this domain. Both facts are printed as WARNING before either
 * button is offered, and refusal asks for a second, explicit confirmation,
 * because it is the only terminal action a customer can take here.
 *
 * What is deliberately absent is anything the API withholds: no assigned
 * mechanic, no diagnosis notes. `CustomerWorkOrderView` carries four fields
 * and this screen shows those four.
 */
@Component({
  selector: 'app-my-job',
  imports: [Callout, RouterLink, StatusMark],
  templateUrl: './my-job.html',
  styleUrls: ['./garage.scss', './my-job.scss'],
})
export class MyJob {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(ShopApi);
  protected readonly store = inject(CustomerStore);

  protected readonly procedure = CUSTOMER_PROCEDURE;
  protected readonly step = customerStepFor;

  protected readonly id = toSignal(this.route.paramMap.pipe(map((p) => p.get('id') ?? '')), {
    initialValue: this.route.snapshot.paramMap.get('id') ?? '',
  });

  protected readonly loading = signal(false);
  protected readonly loadError = signal<string | null>(null);
  protected readonly history = signal<readonly HistoryEntry[]>([]);

  protected readonly pending = signal<Pending>(null);
  protected readonly confirming = signal<Pending>(null);
  protected readonly reason = signal('');
  protected readonly actionError = signal<string | null>(null);

  protected readonly job = computed(() => this.store.job(this.id()) ?? null);

  protected readonly here = computed(() => {
    const job = this.job();
    return job ? this.step(job.status) : null;
  });

  /** A refused job branched off the main line; the rail says so rather than lying about it. */
  protected readonly refused = computed(() => this.job()?.status === 'REFUSED');

  protected readonly decidable = computed(() => {
    const job = this.job();
    return job?.status === 'WAITING_APPROVAL' && job.budget !== null;
  });

  constructor() {
    effect(() => {
      const id = this.id();
      if (id) void this.open(id);
    });
  }

  /**
   * Read the job even when it is not in the local register.
   *
   * This is the path an emailed link takes: a device that has never seen the
   * job at all. `trackJob` reads it first and only files the reference once
   * the shop has answered for it, so a bad link cannot leave a broken row
   * behind.
   */
  private async open(id: string): Promise<void> {
    this.loading.set(true);
    this.loadError.set(null);
    this.confirming.set(null);
    try {
      await this.store.trackJob(id);
      await this.loadHistory(id);
    } catch (error) {
      this.loadError.set(
        error instanceof ApiError && error.isNotFound
          ? 'No job on your account has that reference.'
          : error instanceof ApiError
            ? error.message
            : 'That job could not be read.',
      );
    } finally {
      this.loading.set(false);
    }
  }

  private async loadHistory(id: string): Promise<void> {
    try {
      const page = await this.api.customerWorkOrderHistory(id);
      this.history.set(page.content.map(toHistoryEntry));
    } catch {
      // The timeline is context, not the point of the page. A job that reads
      // fine but whose history does not is still a job worth showing.
      this.history.set([]);
    }
  }

  protected async approve(): Promise<void> {
    const job = this.job();
    if (!job?.budget || this.pending()) return;
    this.pending.set('approve');
    this.actionError.set(null);
    try {
      await this.store.approveBudget(job.id, job.budget.id);
      this.confirming.set(null);
      await this.loadHistory(job.id);
    } catch (error) {
      this.actionError.set(
        error instanceof ApiError ? error.message : 'The approval was not recorded.',
      );
    } finally {
      this.pending.set(null);
    }
  }

  protected async refuse(): Promise<void> {
    const job = this.job();
    if (!job?.budget || this.pending()) return;
    this.pending.set('refuse');
    this.actionError.set(null);
    try {
      await this.store.refuseBudget(job.id, job.budget.id, this.reason());
      this.confirming.set(null);
      this.reason.set('');
      await this.loadHistory(job.id);
    } catch (error) {
      this.actionError.set(
        error instanceof ApiError ? error.message : 'The refusal was not recorded.',
      );
    } finally {
      this.pending.set(null);
    }
  }

  protected async bookPickup(): Promise<void> {
    await this.router.navigate(['/my/booking'], {
      queryParams: { pickup: this.id() },
    });
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
