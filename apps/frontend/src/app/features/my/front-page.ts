import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CustomerStore } from '../../core/customer/customer-store';
import { Session } from '../../core/auth/session';
import { customerStepFor } from '../../core/domain/customer-procedure';
import { Callout } from '../../shared/ui/callout';
import { Icon } from '../../shared/ui/icon';
import { StatusMark } from '../../shared/ui/status-mark';

/**
 * The front page of the owner's manual: what, if anything, is waiting on you.
 *
 * Deliberately not a dashboard. There are no tiles of counts, because a
 * customer with two cars has nothing to aggregate — what they need is the
 * short list of things that stop until they act, and then a plain way in to
 * everything else. When nothing is waiting, the page says so in one line
 * rather than inventing activity to fill itself.
 */
@Component({
  selector: 'app-garage-front',
  imports: [Callout, Icon, RouterLink, StatusMark],
  templateUrl: './front-page.html',
  styleUrls: ['./garage.scss', './front-page.scss'],
})
export class GarageFrontPage {
  protected readonly store = inject(CustomerStore);
  protected readonly session = inject(Session);

  protected readonly step = customerStepFor;

  protected readonly firstName = computed(
    () => this.session.user()?.firstName ?? 'there',
  );

  /** Everything that stops dead until this person does something. */
  protected readonly waiting = computed(() => [
    ...this.store.awaitingDecision(),
    ...this.store.readyForPickup(),
  ]);

  protected readonly nextBooking = computed(() => this.store.upcomingBookings()[0] ?? null);

  protected readonly moving = computed(() =>
    this.store
      .openJobs()
      .filter((j) => !this.waiting().some((w) => w.id === j.id)),
  );

  protected when(instant: string): string {
    return new Date(instant).toLocaleString('en-GB', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  protected money(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }
}
