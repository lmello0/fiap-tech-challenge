import { Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ShopStore } from '../../core/data/shop-store';
import { Session } from '../../core/auth/session';
import {
  BUDGET_STATUS_LABEL,
  FUEL_TYPE_LABEL,
  TRANSMISSION_LABEL,
  UOM_ABBR,
  WORKER_ROLE_LABEL,
} from '../../core/domain/enums';
import { type LifecycleStep, mayAdvance, nextStep, stepFor } from '../../core/domain/lifecycle';
import type { BudgetLine } from '../../core/domain/models';
import { StepRail } from '../../shared/ui/step-rail';
import { StatusMark } from '../../shared/ui/status-mark';
import { Callout } from '../../shared/ui/callout';
import { Icon } from '../../shared/ui/icon';

@Component({
  selector: 'app-work-order-detail',
  imports: [RouterLink, StepRail, StatusMark, Callout, Icon],
  templateUrl: './detail.html',
  styleUrl: './detail.scss',
})
export class WorkOrderDetail {
  /** Bound from the route via `withComponentInputBinding()`. */
  readonly id = input.required<string>();

  private readonly store = inject(ShopStore);
  protected readonly session = inject(Session);

  protected readonly order = computed(() => this.store.workOrder(this.id()));
  protected readonly budget = computed(() => this.store.budgetFor(this.id()));
  protected readonly block = computed(() => this.store.blockFor(this.id()));
  protected readonly history = computed(() => this.store.history(this.id()));

  protected readonly customer = computed(() => {
    const o = this.order();
    return o ? this.store.customer(o.customerId) : undefined;
  });

  protected readonly vehicle = computed(() => {
    const o = this.order();
    return o ? this.store.vehicle(o.vehicleId) : undefined;
  });

  protected readonly step = computed(() => {
    const o = this.order();
    return o ? stepFor(o.status) : null;
  });

  protected readonly next = computed(() => {
    const o = this.order();
    return o ? nextStep(o.status) : null;
  });

  protected readonly canAdvance = computed(() => {
    const o = this.order();
    if (!o) return false;
    if (this.block().blocked && this.next()?.status === 'IN_PROGRESS') return false;
    return mayAdvance(o.status, this.session.role());
  });

  /** A draft budget is the only editable one. Sending freezes it for good. */
  protected readonly editable = computed(() => {
    const b = this.budget();
    return b?.status === 'DRAFT' && this.session.hasAnyRole('MECHANIC', 'MANAGER');
  });

  protected readonly canSend = computed(
    () =>
      (this.budget()?.status === 'DRAFT' || this.budget()?.status === 'WAITING_SEND') &&
      this.session.hasAnyRole('ATTENDANT', 'MANAGER'),
  );

  /** The mechanic's own line timing, only while service is under way. */
  protected readonly canTimeLines = computed(
    () => this.order()?.status === 'IN_PROGRESS' && this.session.hasAnyRole('MECHANIC', 'MANAGER'),
  );

  protected readonly selectedEntry = signal<string | null>(null);
  protected readonly toast = signal<{ tone: 'warn' | 'ok'; text: string } | null>(null);
  protected readonly confirming = signal(false);

  /** Catalogue rows a draft can still draw from. */
  protected readonly addableParts = computed(() => this.store.parts().filter((p) => p.active));
  protected readonly addableServices = computed(() => this.store.services().filter((s) => s.active));

  protected readonly budgetStatusLabel = computed(() => {
    const b = this.budget();
    return b ? BUDGET_STATUS_LABEL[b.status] : '';
  });

  /**
   * Visible consequence propagation: what this line does to the shelf.
   * A draft reservation reduces `available` without moving `quantityOnHand`,
   * so both numbers are shown — the distinction is the whole point.
   */
  protected stockAfter(line: BudgetLine): { onHand: number; available: number; uom: string } | null {
    if (!line.partId) return null;
    const part = this.store.part(line.partId);
    if (!part) return null;
    return { onHand: part.quantityOnHand, available: part.available, uom: UOM_ABBR[part.unitOfMeasure] };
  }

  protected advance(): void {
    const o = this.order();
    if (!o) return;
    const result = this.store.advance(o.id);
    this.confirming.set(false);
    this.toast.set(
      result.ok
        ? { tone: 'ok', text: `Step performed. This order now stands at ${stepFor(this.store.workOrder(o.id)!.status).title.toLowerCase()}.` }
        : { tone: 'warn', text: result.error ?? 'That step could not be performed.' },
    );
    setTimeout(() => this.toast.set(null), 7000);
  }

  protected requestAdvance(): void {
    // A frozen or destructive step is confirmed against its consequence first.
    if (this.next()?.action?.tier === 'warning') this.confirming.set(true);
    else this.advance();
  }

  protected resend(): void {
    const b = this.budget();
    if (!b) return;
    this.store.resendBudget(b.id);
    this.toast.set({ tone: 'ok', text: 'Budget resent. Nothing was re-locked or re-reserved.' });
    setTimeout(() => this.toast.set(null), 7000);
  }

  protected removeLine(lineId: string): void {
    const b = this.budget();
    if (b) this.store.removeLine(b.id, lineId);
  }

  protected changeQuantity(lineId: string, delta: number): void {
    const b = this.budget();
    const line = b?.lines.find((l) => l.id === lineId);
    if (b && line) this.store.setLineQuantity(b.id, lineId, line.quantity + delta);
  }

  protected addPart(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const part = this.store.part(select.value);
    const b = this.budget();
    if (part && b) {
      this.store.addLine(b.id, {
        type: 'PART',
        description: part.name,
        quantity: 1,
        unitPrice: part.salePrice,
        partId: part.id,
        serviceId: null,
        startedAt: null,
        finishedAt: null,
      });
    }
    select.value = '';
  }

  protected addService(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const service = this.store.services().find((s) => s.id === select.value);
    const b = this.budget();
    if (service && b) {
      this.store.addLine(b.id, {
        type: 'SERVICE',
        description: service.name,
        quantity: 1,
        unitPrice: service.price,
        partId: null,
        serviceId: service.id,
        startedAt: null,
        finishedAt: null,
      });
    }
    select.value = '';
  }

  protected startLine(lineId: string): void {
    const b = this.budget();
    if (b) this.store.startLine(b.id, lineId);
  }

  protected finishLine(lineId: string): void {
    const b = this.budget();
    if (b) this.store.finishLine(b.id, lineId);
  }

  protected daysInShop(): number {
    const o = this.order();
    if (!o) return 0;
    return Math.max(0, Math.floor((this.store.now.getTime() - new Date(o.createdAt).getTime()) / 86_400_000));
  }

  /** "an attendant or manager" — reads as prose in the inert notice. */
  protected roleWord(step: LifecycleStep): string {
    const roles = step.action?.roles ?? [];
    return roles.map((r) => WORKER_ROLE_LABEL[r].toLowerCase()).join(' or ');
  }

  protected fuelLabel(f: keyof typeof FUEL_TYPE_LABEL): string {
    return FUEL_TYPE_LABEL[f];
  }

  protected transmissionLabel(t: keyof typeof TRANSMISSION_LABEL): string {
    return TRANSMISSION_LABEL[t];
  }

  /** Money stays in the shop's real currency; dates follow the UI language. */
  protected money(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  protected when(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
