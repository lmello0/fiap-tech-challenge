import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormField, form, minLength, required, schema, validate } from '@angular/forms/signals';
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
import { type LifecycleStep, mayAdvance, mayCancel, nextStep, stepFor } from '../../core/domain/lifecycle';
import type { BudgetLine } from '../../core/domain/models';
import { StepRail } from '../../shared/ui/step-rail';
import { StatusMark } from '../../shared/ui/status-mark';
import { Callout } from '../../shared/ui/callout';
import { Icon } from '../../shared/ui/icon';
import { EntryBand } from '../../shared/ui/entry-band';
import { FormFieldRow } from '../../shared/ui/form-field';

/** One seeded budget line, as the diagnosis form collects it. */
interface SeedLine {
  kind: 'SERVICE' | 'PART';
  refId: string;
  quantity: number;
}

interface DiagnosisDraft {
  diagnosis: string;
  lines: SeedLine[];
}

const diagnosisSchema = schema<DiagnosisDraft>((p) => {
  required(p.diagnosis, { message: 'A written diagnosis is required.' });
  minLength(p.diagnosis, 10, { message: 'Say what is wrong, in a sentence the customer could read.' });
  // The API refuses an empty budget, so the form does too — and says why.
  validate(p.lines, (ctx) =>
    ctx.value().length === 0
      ? { kind: 'lines', message: 'A budget needs at least one line.' }
      : null,
  );
});

@Component({
  selector: 'app-work-order-detail',
  imports: [RouterLink, StepRail, StatusMark, Callout, Icon, EntryBand, FormFieldRow, FormField],
  templateUrl: './detail.html',
  styleUrl: './detail.scss',
})
export class WorkOrderDetail {
  /** Bound from the route via `withComponentInputBinding()`. */
  readonly id = input.required<string>();

  private readonly store = inject(ShopStore);
  protected readonly session = inject(Session);

  constructor() {
    // The Timeline is the one read the board does not already hold, so it is
    // fetched per order rather than with everything else. Re-runs when the
    // route's id changes, which is how the previous/next links move between orders.
    effect((onCleanup) => {
      const id = this.id();
      if (!id) return;

      // The board's query may not include this order — it is reachable by link —
      // so the detail view makes sure of its own record before reading it.
      //
      // The second read is guarded because the first is awaited: navigating away
      // (or signing out) between them would otherwise send a request for a view
      // that no longer exists, against a session that may no longer be valid.
      let live = true;
      onCleanup(() => {
        live = false;
      });

      void this.store.ensureWorkOrder(id).then(() => {
        if (live) void this.store.loadHistory(id);
      });
    });
  }

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

  protected readonly canCancel = computed(() => {
    const o = this.order();
    return o ? mayCancel(o.status, this.session.role()) : false;
  });

  protected readonly cancelling = signal(false);
  protected readonly cancelReason = signal('');
  protected readonly cancelBusy = signal(false);
  protected readonly cancelError = signal<string | null>(null);

  protected openCancel(): void {
    this.cancelReason.set('');
    this.cancelError.set(null);
    this.cancelling.set(true);
  }

  protected closeCancel(): void {
    this.cancelling.set(false);
    this.cancelError.set(null);
  }

  protected onCancelReason(event: Event): void {
    this.cancelReason.set((event.target as HTMLTextAreaElement).value);
  }

  protected async confirmCancel(): Promise<void> {
    const o = this.order();
    if (!o) return;
    this.cancelBusy.set(true);
    this.cancelError.set(null);
    const result = await this.store.cancelWorkOrder(o.id, this.cancelReason().trim() || null);
    this.cancelBusy.set(false);
    if (!result.ok) {
      this.cancelError.set(result.error ?? 'This work order could not be cancelled.');
      return;
    }
    this.cancelling.set(false);
    this.toast.set({ tone: 'ok', text: 'Work order cancelled.' });
    setTimeout(() => this.toast.set(null), 7000);
  }

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

  protected async advance(): Promise<void> {
    const o = this.order();
    if (!o) return;
    const result = await this.store.advance(o.id);
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
    else void this.advance();
  }

  protected async resend(): Promise<void> {
    const b = this.budget();
    if (!b) return;
    const result = await this.store.resendBudget(b.id);
    this.toast.set(
      result.ok
        ? { tone: 'ok', text: 'Budget resent. Nothing was re-locked or re-reserved.' }
        : { tone: 'warn', text: result.error ?? 'The budget could not be resent.' },
    );
    setTimeout(() => this.toast.set(null), 7000);
  }

  protected async removeLine(lineId: string): Promise<void> {
    const b = this.budget();
    if (b) await this.report(this.store.removeLine(b.id, lineId));
  }

  protected async changeQuantity(lineId: string, delta: number): Promise<void> {
    const b = this.budget();
    const line = b?.lines.find((l) => l.id === lineId);
    if (b && line) await this.report(this.store.setLineQuantity(b.id, lineId, line.quantity + delta));
  }

  protected async addPart(event: Event): Promise<void> {
    const select = event.target as HTMLSelectElement;
    const part = this.store.part(select.value);
    const b = this.budget();
    select.value = '';
    if (part && b) {
      await this.report(
        this.store.addLine(b.id, {
          type: 'PART',
          quantity: 1,
          partId: part.id,
          description: part.name,
          unitPrice: part.salePrice,
        }),
      );
    }
  }

  protected async addService(event: Event): Promise<void> {
    const select = event.target as HTMLSelectElement;
    const service = this.store.services().find((s) => s.id === select.value);
    const b = this.budget();
    select.value = '';
    if (service && b) {
      await this.report(
        this.store.addLine(b.id, {
          type: 'SERVICE',
          quantity: 1,
          serviceId: service.id,
          description: service.name,
          unitPrice: service.price,
        }),
      );
    }
  }

  protected async startLine(lineId: string): Promise<void> {
    const b = this.budget();
    if (b) await this.report(this.store.startLine(b.id, lineId));
  }

  protected async finishLine(lineId: string): Promise<void> {
    const b = this.budget();
    if (b) await this.report(this.store.finishLine(b.id, lineId));
  }

  /** A refused edit is said out loud; a successful one is visible in the table. */
  private async report(work: Promise<{ ok: boolean; error?: string }>): Promise<void> {
    const result = await work;
    if (result.ok) return;
    this.toast.set({ tone: 'warn', text: result.error ?? 'That edit was refused.' });
    setTimeout(() => this.toast.set(null), 7000);
  }

  /**
   * How long the vehicle has been here — or was, once it has gone. A closed
   * order that still reads "in shop" describes a car that is not on the premises.
   */
  protected dwellLabel(): string {
    const o = this.order();
    if (!o) return '';
    const days = this.daysInShop();
    const span = days === 0 ? 'same day' : `${days}d`;
    if (o.status === 'DELIVERED') return `Closed in ${span}`;
    if (o.status === 'REFUSED') return `Refused after ${span}`;
    if (o.status === 'CANCELLED') return `Cancelled after ${span}`;
    return days === 0 ? 'In shop today' : `In shop ${span}`;
  }

  protected daysInShop(): number {
    const o = this.order();
    if (!o) return 0;
    return Math.max(0, Math.floor((this.store.now.getTime() - new Date(o.createdAt).getTime()) / 86_400_000));
  }

  /** "an attendant or manager" — reads as prose in the inert notice. */
  protected roleLabel(role: keyof typeof WORKER_ROLE_LABEL): string {
    return WORKER_ROLE_LABEL[role];
  }

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

  /* ---------------------------------------------------------------------
     Step 3 — take the job.

     The backend assigns the mechanic as part of starting diagnostics, so the
     picker and the action are one band. `/workers` is MANAGER-only, so a
     mechanic sees only themselves here; that is the role matrix, not a gap.
     --------------------------------------------------------------------- */

  protected readonly isDemo = this.store.isDemo;
  protected readonly assigning = signal(false);
  protected readonly diagnosing = signal(false);
  protected readonly bandBusy = signal(false);
  protected readonly bandError = signal<string | null>(null);

  protected readonly mechanics = computed(() => {
    const roster = this.store
      .workers()
      .filter((w) => w.active && (w.role === 'MECHANIC' || w.role === 'MANAGER'));
    if (roster.length > 0) return roster;
    // Without the roster, the only person this operator can assign is themselves.
    const me = this.session.worker();
    return me ? [me] : [];
  });

  protected readonly assignee = signal<string>('');

  protected openAssign(): void {
    const current = this.order()?.assignedMechanicId;
    this.assignee.set(current ?? this.session.worker()?.id ?? this.mechanics()[0]?.id ?? '');
    this.bandError.set(null);
    this.assigning.set(true);
  }

  protected onAssignee(event: Event): void {
    this.assignee.set((event.target as HTMLSelectElement).value);
  }

  protected async confirmAssign(): Promise<void> {
    const o = this.order();
    const who = this.assignee();
    if (!o || !who) return;
    this.bandBusy.set(true);
    this.bandError.set(null);
    const result = await this.store.startDiagnostics(o.id, who);
    this.bandBusy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'Diagnostics could not be started.');
      return;
    }
    this.assigning.set(false);
    this.toast.set({ tone: 'ok', text: 'Diagnostics started. The job is now step 3.' });
    setTimeout(() => this.toast.set(null), 6000);
  }

  /* ---------------------------------------------------------------------
     Step 4 — record the diagnosis and open the budget.

     One call does both: the backend records the diagnosis and drafts the budget
     atomically, seeded with these lines. Adding a part line reserves stock the
     moment this saves, which is why the shelf position is shown against each.
     --------------------------------------------------------------------- */

  protected readonly diagnosisDraft = signal<DiagnosisDraft>({ diagnosis: '', lines: [] });
  protected readonly df = form(this.diagnosisDraft, diagnosisSchema);

  protected readonly availableParts = computed(() => this.store.parts().filter((p) => p.active));
  protected readonly availableServices = computed(() =>
    this.store.services().filter((s) => s.active),
  );

  protected readonly hasCatalogue = computed(
    () => this.availableParts().length > 0 || this.availableServices().length > 0,
  );

  protected openDiagnosis(): void {
    this.diagnosisDraft.set({ diagnosis: this.order()?.diagnosis ?? '', lines: [] });
    this.bandError.set(null);
    this.diagnosing.set(true);
  }

  protected closeBands(): void {
    this.assigning.set(false);
    this.diagnosing.set(false);
    this.bandError.set(null);
  }

  protected addSeedLine(kind: 'SERVICE' | 'PART', event: Event): void {
    const select = event.target as HTMLSelectElement;
    const refId = select.value;
    select.value = '';
    if (!refId) return;
    this.diagnosisDraft.update((d) => ({ ...d, lines: [...d.lines, { kind, refId, quantity: 1 }] }));
  }

  protected removeSeedLine(index: number): void {
    this.diagnosisDraft.update((d) => ({ ...d, lines: d.lines.filter((_, i) => i !== index) }));
  }

  protected changeSeedQuantity(index: number, delta: number): void {
    this.diagnosisDraft.update((d) => ({
      ...d,
      lines: d.lines.map((l, i) =>
        i === index ? { ...l, quantity: Math.max(1, l.quantity + delta) } : l,
      ),
    }));
  }

  protected seedLabel(line: SeedLine): string {
    return line.kind === 'PART'
      ? (this.store.part(line.refId)?.name ?? 'Unknown part')
      : (this.store.services().find((s) => s.id === line.refId)?.name ?? 'Unknown service');
  }

  protected seedUnitPrice(line: SeedLine): number {
    return line.kind === 'PART'
      ? (this.store.part(line.refId)?.salePrice ?? 0)
      : (this.store.services().find((s) => s.id === line.refId)?.price ?? 0);
  }

  /** What this line will do to the shelf the moment the budget is drafted. */
  protected seedShelf(line: SeedLine): string | null {
    if (line.kind !== 'PART') return null;
    const part = this.store.part(line.refId);
    if (!part) return null;
    const after = part.available - line.quantity;
    return `${part.available} available → ${after} after reserving`;
  }

  protected readonly seedTotal = computed(() =>
    this.diagnosisDraft().lines.reduce((sum, l) => sum + l.quantity * this.seedUnitPrice(l), 0),
  );

  protected async saveDiagnosis(): Promise<void> {
    const o = this.order();
    if (!o || this.df().invalid()) return;
    this.bandBusy.set(true);
    this.bandError.set(null);
    const d = this.diagnosisDraft();
    const result = await this.store.finishDiagnostics(
      o.id,
      d.diagnosis.trim(),
      d.lines.map((l) => ({
        type: l.kind,
        quantity: l.quantity,
        partId: l.kind === 'PART' ? l.refId : undefined,
        serviceId: l.kind === 'SERVICE' ? l.refId : undefined,
      })),
    );
    this.bandBusy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'The diagnosis could not be recorded.');
      return;
    }
    this.diagnosing.set(false);
    this.toast.set({
      tone: 'ok',
      text: 'Diagnosis recorded and the budget drafted. Parts on it are now reserved.',
    });
    setTimeout(() => this.toast.set(null), 7000);
  }
}
