import { Component, computed, inject, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import {
  FormField,
  disabled,
  form,
  min,
  minLength,
  required,
  schema,
  validate,
} from '@angular/forms/signals';
import { ShopStore } from '../../core/data/shop-store';
import { Session } from '../../core/auth/session';
import { EntryBand } from '../../shared/ui/entry-band';
import { FormFieldRow } from '../../shared/ui/form-field';
import type { Part, PurchaseOrder, RepairService, Vendor } from '../../core/domain/models';
import type { UnitOfMeasure } from '../../core/domain/enums';
import { PURCHASE_ORDER_STATUS_LABEL, STOCK_MOVEMENT_LABEL, UOM_ABBR } from '../../core/domain/enums';
import { Callout } from '../../shared/ui/callout';
import { Icon } from '../../shared/ui/icon';

type Tab = 'parts' | 'services' | 'vendors' | 'purchasing' | 'ledger';

interface PartDraft {
  creating: boolean;
  sku: string;
  name: string;
  brand: string;
  description: string;
  unitOfMeasure: UnitOfMeasure;
  salePrice: number | null;
}

const EMPTY_PART: PartDraft = {
  creating: true,
  sku: '',
  name: '',
  brand: '',
  description: '',
  unitOfMeasure: 'UNIT',
  salePrice: null,
};

const partSchema = schema<PartDraft>((p) => {
  required(p.sku, { message: 'A SKU is required.' });
  required(p.name, { message: 'A name is required.' });
  required(p.salePrice, { message: 'A sale price is required.' });
  min(p.salePrice, 0, { message: 'A price cannot be negative.' });
  // The SKU is the part's identity in the ledger and the API will not change it.
  disabled(p.sku, { when: (ctx) => !ctx.valueOf(p.creating) });
});

interface ServiceDraft {
  creating: boolean;
  code: string;
  name: string;
  description: string;
  price: number | null;
  estimatedMinutes: number | null;
}

const EMPTY_SERVICE: ServiceDraft = {
  creating: true,
  code: '',
  name: '',
  description: '',
  price: null,
  estimatedMinutes: null,
};

const serviceSchema = schema<ServiceDraft>((p) => {
  required(p.code, { message: 'A code is required.' });
  required(p.name, { message: 'A name is required.' });
  required(p.price, { message: 'A price is required.' });
  min(p.price, 0, { message: 'A price cannot be negative.' });
  required(p.estimatedMinutes, { message: 'An estimate is required.' });
  min(p.estimatedMinutes, 1, { message: 'At least a minute.' });
  disabled(p.code, { when: (ctx) => !ctx.valueOf(p.creating) });
});

interface VendorDraft {
  name: string;
  contactEmail: string;
}

const EMPTY_VENDOR: VendorDraft = { name: '', contactEmail: '' };

const vendorSchema = schema<VendorDraft>((p) => {
  required(p.name, { message: 'A vendor needs a name.' });
});

/** A threshold belongs to a part, so the band opens on the part's own row. */
interface PolicyDraft {
  minQuantity: number | null;
  maxQuantity: number | null;
  vendorId: string;
  autoReorderEnabled: boolean;
}

const EMPTY_POLICY: PolicyDraft = {
  minQuantity: null,
  maxQuantity: null,
  vendorId: '',
  autoReorderEnabled: true,
};

const policySchema = schema<PolicyDraft>((p) => {
  required(p.minQuantity, { message: 'A minimum is required — it is what triggers the reorder.' });
  min(p.minQuantity, 0, { message: 'A minimum cannot be negative.' });
  min(p.maxQuantity, 0, { message: 'A maximum cannot be negative.' });
  validate(p.maxQuantity, (ctx) => {
    const max = ctx.value();
    const lo = ctx.valueOf(p.minQuantity);
    if (max === null || lo === null) return null;
    return max >= lo ? null : { kind: 'range', message: 'The maximum must be at or above the minimum.' };
  });
  // Auto-reorder places a real order, so it needs somewhere to send it.
  validate(p.vendorId, (ctx) =>
    ctx.valueOf(p.autoReorderEnabled) && !ctx.value()
      ? { kind: 'vendor', message: 'Automatic reordering needs a vendor to order from.' }
      : null,
  );
});

interface OrderLineDraft {
  partId: string;
  quantity: number;
}

interface OrderDraft {
  vendorId: string;
  lines: OrderLineDraft[];
}

const orderSchema = schema<OrderDraft>((p) => {
  required(p.vendorId, { message: 'Which vendor is this going to?' });
  validate(p.lines, (ctx) =>
    ctx.value().length === 0 ? { kind: 'lines', message: 'An order needs at least one line.' } : null,
  );
});

interface AdjustDraft {
  quantity: number | null;
  reason: string;
}

const EMPTY_ADJUST: AdjustDraft = { quantity: null, reason: '' };

const adjustSchema = schema<AdjustDraft>((p) => {
  required(p.quantity, { message: 'A quantity is required.' });
  // Signed on purpose: positive adds, negative removes. Zero is refused by the API.
  validate(p.quantity, (ctx) =>
    ctx.value() === 0 ? { kind: 'zero', message: 'An adjustment of zero changes nothing.' } : null,
  );
  required(p.reason, { message: 'A reason is required — the ledger is append-only.' });
  minLength(p.reason, 3, { message: 'Say what happened, briefly.' });
});

@Component({
  selector: 'app-inventory',
  imports: [Callout, Icon, EntryBand, FormFieldRow, FormField, NgTemplateOutlet],
  templateUrl: './inventory.html',
  styles: `
    :host {
      display: block;
    }

    .plate {
      border-top: 3px solid var(--section-ink, var(--ink));
    }

    .plate__meta {
      color: var(--ink-3);
    }

    .inv__head {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 2rem;
      flex-wrap: wrap;
      margin-bottom: 1rem;
    }

    .inv__title {
      font-size: var(--step-4);
      font-weight: 700;
      letter-spacing: -0.02em;
      line-height: 1;
    }

    .inv__sub {
      margin-top: 0.35rem;
      color: var(--ink-2);
    }

    .inv__readonly {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      font-size: var(--step--1);
      color: var(--ink-3);
      border: 1px dashed var(--rule-strong);
      padding: 0.3rem 0.55rem;
    }

    /* section sub-tabs, ruled rather than pilled */
    .inv__tabs {
      display: flex;
      gap: 0;
      margin-bottom: 1.25rem;
      border-bottom: 1px solid var(--ink);
    }

    .inv__tab {
      background: transparent;
      border: 1px solid var(--rule);
      border-bottom: 0;
      margin-bottom: -1px;
      padding: 0.4rem 0.9rem;
      font-family: var(--face-cond);
      font-size: var(--step-0);
      font-weight: 600;
      letter-spacing: 0.05em;
      text-transform: uppercase;
      color: var(--ink-3);
      cursor: pointer;
    }

    .inv__tab + .inv__tab {
      margin-left: -1px;
    }

    .inv__tab:hover {
      color: var(--ink);
    }

    .inv__tab.is-active {
      background: var(--plate);
      border-color: var(--ink);
      color: var(--ink);
    }

    .inv__brand,
    .inv__cost {
      color: var(--ink-2);
    }

    .inv__short {
      color: var(--warn);
      font-weight: 600;
    }

    .inv__low {
      color: var(--caution-ink);
      font-weight: 600;
    }

    .inv__flag {
      font-family: var(--face-cond);
      font-size: 0.64rem;
      font-weight: 700;
      letter-spacing: 0.09em;
      text-transform: uppercase;
      color: var(--ink-3);
      border: 1px solid var(--rule-strong);
      padding: 0 0.25rem;
      margin-left: 0.35rem;
    }

    tr.is-inactive td {
      color: var(--ink-3);
    }

    .inv__rule {
      font-family: var(--face-mono);
      font-size: 0.72rem;
      color: var(--ink-2);
    }

    .inv__rule.is-off {
      text-decoration: line-through;
      color: var(--ink-3);
    }

    .inv__norule {
      font-size: 0.72rem;
      color: var(--ink-3);
    }

    /* An estimate is a basis, not a withdrawn value. Struck-through type says
       "no longer applies", which is the opposite of what an estimate means. */
    .inv__basis {
      font-family: var(--face-cond);
      font-size: var(--step--1);
      font-weight: 600;
      letter-spacing: 0.05em;
      text-transform: uppercase;
      color: var(--caution-ink);
    }

    .inv__basis.is-measured {
      color: var(--ok);
    }

    .inv__notice {
      margin-top: 1.25rem;
    }

    .inv__notice-lead {
      font-weight: 600;
    }

    .inv__poline {
      display: block;
      font-size: 0.8rem;
    }

    .inv__status,
    .inv__mv {
      font-family: var(--face-cond);
      font-size: var(--step--1);
      font-weight: 600;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      border: 1px solid currentColor;
      padding: 0.05rem 0.35rem;
      white-space: nowrap;
      color: var(--ink-2);
    }

    .inv__status.is-received,
    .inv__mv.is-purchase {
      color: var(--ok);
      background: var(--ok-field);
    }
    .inv__status.is-partially_received {
      color: var(--caution-ink);
      background: var(--caution-field);
    }
    .inv__status.is-cancelled {
      color: var(--ink-3);
    }
    .inv__status.is-placed {
      color: var(--ref);
      background: var(--ref-field);
    }
    .inv__mv.is-consumption {
      color: var(--ref);
      background: var(--ref-field);
    }
    .inv__mv.is-adjustment {
      color: var(--caution-ink);
      background: var(--caution-field);
    }

    .inv__reason {
      font-style: italic;
      color: var(--ink-2);
      font-size: 0.82rem;
    }

    /* --- entry ---------------------------------------------------------------- */

    .inv__band-row > td {
      padding: 0 !important;
      background: transparent !important;
    }

    /* --- purchasing and thresholds -------------------------------------------- */

    .inv__rule-btn {
      background: none;
      border: 1px dashed var(--rule-strong);
      padding: 0.1rem 0.35rem;
      cursor: pointer;
      color: inherit;
      font: inherit;
    }

    .inv__rule-btn:hover:not(:disabled) {
      border-color: var(--ref);
      border-style: solid;
    }

    .inv__rule-btn:disabled {
      border-color: var(--rule);
      cursor: default;
    }

    .inv__policy-delete {
      margin-left: 0.5rem;
    }

    .inv__check {
      display: flex;
      align-items: flex-start;
      gap: 0.45rem;
      max-width: 60ch;
    }

    .inv__order-lines,
    .inv__receipt {
      border-top: 1px solid var(--rule);
      padding-top: 0.7rem;
    }

    .inv__order-row {
      display: grid;
      grid-template-columns: 9rem minmax(8rem, 1fr) auto minmax(11rem, auto) auto;
      align-items: center;
      gap: 0.6rem;
      padding: 0.35rem 0;
      border-bottom: 1px solid var(--rule);
    }

    .inv__order-name {
      color: var(--ink);
    }

    .inv__order-hint {
      color: var(--ink-3);
      font-size: var(--step--1);
    }

    /* The slack sits between what arrived and what is being entered, so the
       running count stays beside the part it describes. */
    .inv__receipt-row {
      display: grid;
      grid-template-columns: 9rem auto 1fr 10rem 10rem;
      align-items: end;
      gap: 0.6rem;
      padding: 0.4rem 0;
      border-bottom: 1px solid var(--rule);
    }

    .inv__receipt-owed {
      justify-self: start;
      color: var(--ink-2);
      font-size: var(--step--1);
    }

    .inv__receipt-field {
      display: grid;
      gap: 0.2rem;
    }

    .inv__settled {
      color: var(--ink-3);
    }
  `,
})
export class Inventory {
  private readonly store = inject(ShopStore);
  private readonly session = inject(Session);

  protected readonly tabs: { id: Tab; label: string }[] = [
    { id: 'parts', label: 'Parts & stock' },
    { id: 'services', label: 'Labour catalogue' },
    { id: 'vendors', label: 'Vendors' },
    { id: 'purchasing', label: 'Purchasing' },
    { id: 'ledger', label: 'Movements' },
  ];

  protected readonly tab = signal<Tab>('parts');

  protected readonly parts = this.store.parts;
  protected readonly services = this.store.services;
  protected readonly purchaseOrders = this.store.purchaseOrders;
  protected readonly movements = this.store.movements;

  /** A mechanic may read the catalogue but not write to it. */
  protected readonly readOnly = computed(() => this.session.role() === 'MECHANIC');

  protected readonly negative = computed(() => this.store.parts().filter((p) => p.available < 0));

  protected readonly belowMin = computed(() =>
    this.store.parts().filter((p) => {
      const r = this.rule(p.id);
      return r?.enabled ? p.available <= r.min : false;
    }),
  );

  protected rule(partId: string) {
    return this.store.reorderRules().find((r) => r.partId === partId);
  }

  protected isLow(partId: string, available: number): boolean {
    const r = this.rule(partId);
    return !!r?.enabled && available <= r.min;
  }

  /** A purchase order names its vendor by id; the vendor list has the name. */
  protected vendorName(vendorId: string): string {
    return this.store.vendor(vendorId)?.name ?? 'Unknown vendor';
  }

  protected partSku(partId: string): string {
    return this.store.part(partId)?.sku ?? '—';
  }

  protected partName(partId: string): string {
    return this.store.part(partId)?.name ?? 'Unknown part';
  }

  protected uom(u: keyof typeof UOM_ABBR): string {
    return UOM_ABBR[u];
  }

  protected poLabel(s: keyof typeof PURCHASE_ORDER_STATUS_LABEL): string {
    return PURCHASE_ORDER_STATUS_LABEL[s];
  }

  protected mvLabel(t: keyof typeof STOCK_MOVEMENT_LABEL): string {
    return STOCK_MOVEMENT_LABEL[t];
  }

  protected money(v: number): string {
    return v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  protected date(iso: string): string {
    return new Date(iso).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: '2-digit' });
  }

  /* ---------------------------------------------------------------------
     Entry — parts, labour, and corrections to the shelf.
     --------------------------------------------------------------------- */

  protected readonly isDemo = this.store.isDemo;
  protected readonly canWrite = computed(() => this.session.hasAnyRole('STOCKIST', 'MANAGER'));

  protected readonly openPart = signal<string | null>(null);
  protected readonly openService = signal<string | null>(null);
  /** The part whose shelf is being corrected — a separate band from its record. */
  protected readonly adjusting = signal<string | null>(null);
  protected readonly withdrawing = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly bandError = signal<string | null>(null);

  protected readonly partDraft = signal<PartDraft>({ ...EMPTY_PART });
  protected readonly pf = form(this.partDraft, partSchema);

  protected readonly serviceDraft = signal<ServiceDraft>({ ...EMPTY_SERVICE });
  protected readonly sf = form(this.serviceDraft, serviceSchema);

  protected readonly adjustDraft = signal<AdjustDraft>({ ...EMPTY_ADJUST });
  protected readonly af = form(this.adjustDraft, adjustSchema);

  protected readonly uoms: [UnitOfMeasure, string][] = [
    ['UNIT', 'Unit'],
    ['LITER', 'Litre'],
    ['KILOGRAM', 'Kilogram'],
    ['METER', 'Metre'],
    ['SET', 'Set'],
  ];

  /** What the shelf will read after the pending correction is applied. */
  protected readonly adjustPreview = computed(() => {
    const id = this.adjusting();
    const part = id ? this.store.part(id) : undefined;
    const delta = this.adjustDraft().quantity;
    if (!part || delta === null) return null;
    const onHand = part.quantityOnHand + delta;
    return { onHand, available: onHand - part.quantityReserved, uom: this.uom(part.unitOfMeasure) };
  });

  private closeAll(): void {
    this.openPart.set(null);
    this.openService.set(null);
    this.adjusting.set(null);
    this.withdrawing.set(null);
    this.bandError.set(null);
  }

  protected closeBands(): void {
    this.closeAll();
  }

  protected newPart(): void {
    this.closeAll();
    this.partDraft.set({ ...EMPTY_PART });
    this.openPart.set('new');
  }

  protected editPart(p: Part): void {
    this.closeAll();
    this.partDraft.set({
      creating: false,
      sku: p.sku,
      name: p.name,
      brand: p.brand ?? '',
      description: p.description ?? '',
      unitOfMeasure: p.unitOfMeasure,
      salePrice: p.salePrice,
    });
    this.openPart.set(p.id);
  }

  protected async savePart(): Promise<void> {
    const target = this.openPart();
    if (!target || this.pf().invalid()) return;
    this.busy.set(true);
    this.bandError.set(null);
    const d = this.partDraft();
    const command = {
      name: d.name.trim(),
      brand: d.brand.trim() || null,
      description: d.description.trim() || null,
      unitOfMeasure: d.unitOfMeasure,
      salePrice: d.salePrice!,
    };
    const result =
      target === 'new'
        ? await this.store.createPart({ ...command, sku: d.sku.trim().toUpperCase() })
        : await this.store.updatePart(target, command);
    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That part could not be saved.');
      return;
    }
    this.closeAll();
  }

  protected openAdjust(p: Part): void {
    this.closeAll();
    this.adjustDraft.set({ ...EMPTY_ADJUST });
    this.adjusting.set(p.id);
  }

  protected async saveAdjust(): Promise<void> {
    const id = this.adjusting();
    if (!id || this.af().invalid()) return;
    this.busy.set(true);
    this.bandError.set(null);
    const d = this.adjustDraft();
    const result = await this.store.adjustStock(id, d.quantity!, d.reason);
    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That adjustment was refused.');
      return;
    }
    this.closeAll();
  }

  protected async withdrawPart(p: Part): Promise<void> {
    this.busy.set(true);
    const result = await this.store.deactivatePart(p.id);
    this.busy.set(false);
    if (!result.ok) this.bandError.set(result.error ?? null);
    else this.closeAll();
  }

  protected newService(): void {
    this.closeAll();
    this.serviceDraft.set({ ...EMPTY_SERVICE });
    this.openService.set('new');
  }

  protected editService(s: RepairService): void {
    this.closeAll();
    this.serviceDraft.set({
      creating: false,
      code: s.code,
      name: s.name,
      description: s.description ?? '',
      price: s.price,
      estimatedMinutes: s.executionMinutes,
    });
    this.openService.set(s.id);
  }

  protected async saveService(): Promise<void> {
    const target = this.openService();
    if (!target || this.sf().invalid()) return;
    this.busy.set(true);
    this.bandError.set(null);
    const d = this.serviceDraft();
    const command = {
      name: d.name.trim(),
      description: d.description.trim() || null,
      price: d.price!,
      estimatedSeconds: Math.round(d.estimatedMinutes! * 60),
    };
    const result =
      target === 'new'
        ? await this.store.createService({ ...command, code: d.code.trim().toUpperCase() })
        : await this.store.updateService(target, command);
    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That service could not be saved.');
      return;
    }
    this.closeAll();
  }

  protected async withdrawService(s: RepairService): Promise<void> {
    this.busy.set(true);
    const result = await this.store.deactivateService(s.id);
    this.busy.set(false);
    if (!result.ok) this.bandError.set(result.error ?? null);
    else this.closeAll();
  }

  protected minutes(m: number | null): string {
    if (m === null) return '—';
    if (m < 60) return `${m}m`;
    return `${Math.floor(m / 60)}h ${String(m % 60).padStart(2, '0')}m`;
  }

  /* ---------------------------------------------------------------------
     Vendors, thresholds and purchasing.
     --------------------------------------------------------------------- */

  protected readonly vendors = this.store.vendors;
  protected readonly reorderRules = this.store.reorderRules;

  protected readonly openVendor = signal<string | null>(null);
  protected readonly deactivatingVendor = signal<string | null>(null);
  protected readonly openPolicy = signal<string | null>(null);
  protected readonly placingOrder = signal(false);
  protected readonly receiving = signal<string | null>(null);
  protected readonly cancelling = signal<string | null>(null);

  protected readonly vendorDraft = signal<VendorDraft>({ ...EMPTY_VENDOR });
  protected readonly vf = form(this.vendorDraft, vendorSchema);

  protected readonly policyDraft = signal<PolicyDraft>({ ...EMPTY_POLICY });
  protected readonly plf = form(this.policyDraft, policySchema);

  protected readonly orderDraft = signal<OrderDraft>({ vendorId: '', lines: [] });
  protected readonly of = form(this.orderDraft, orderSchema);

  /** What is being received, keyed by line id: quantity and the cost it arrived at. */
  protected readonly receipt = signal<Record<string, { quantity: number; unitCost: number }>>({});

  protected readonly activeVendors = computed(() =>
    this.store.vendors().filter((v) => v.active).sort((a, b) => a.name.localeCompare(b.name)),
  );

  protected readonly orderableParts = computed(() =>
    this.store.parts().filter((p) => p.active),
  );

  private closeInventoryBands(): void {
    this.openVendor.set(null);
    this.deactivatingVendor.set(null);
    this.openPolicy.set(null);
    this.placingOrder.set(false);
    this.receiving.set(null);
    this.cancelling.set(null);
    this.bandError.set(null);
  }

  protected closeAllBands(): void {
    this.closeBands();
    this.closeInventoryBands();
  }

  /* --- vendors ---------------------------------------------------------- */

  protected newVendor(): void {
    this.closeAllBands();
    this.vendorDraft.set({ ...EMPTY_VENDOR });
    this.openVendor.set('new');
  }

  protected editVendor(v: Vendor): void {
    this.closeAllBands();
    this.vendorDraft.set({ name: v.name, contactEmail: v.email ?? '' });
    this.openVendor.set(v.id);
  }

  protected async saveVendor(): Promise<void> {
    const target = this.openVendor();
    if (!target || this.vf().invalid()) return;
    this.busy.set(true);
    this.bandError.set(null);
    const d = this.vendorDraft();
    const command = { name: d.name.trim(), contactEmail: d.contactEmail.trim() || null };
    const result =
      target === 'new'
        ? await this.store.createVendor(command)
        : await this.store.updateVendor(target, command);
    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That vendor could not be saved.');
      return;
    }
    this.closeAllBands();
  }

  protected async deactivateVendor(v: Vendor): Promise<void> {
    this.busy.set(true);
    const result = await this.store.deactivateVendor(v.id);
    this.busy.set(false);
    if (!result.ok) this.bandError.set(result.error ?? null);
    else this.closeAllBands();
  }

  /* --- reorder thresholds ----------------------------------------------- */

  protected openPolicyFor(p: Part): void {
    this.closeAllBands();
    const existing = this.rule(p.id);
    this.policyDraft.set(
      existing
        ? {
            minQuantity: existing.min,
            maxQuantity: existing.max,
            vendorId: existing.vendorId ?? '',
            autoReorderEnabled: existing.enabled,
          }
        : { ...EMPTY_POLICY, vendorId: this.activeVendors()[0]?.id ?? '' },
    );
    this.openPolicy.set(p.id);
  }

  protected async savePolicy(partId: string): Promise<void> {
    if (this.plf().invalid()) return;
    this.busy.set(true);
    this.bandError.set(null);
    const d = this.policyDraft();
    const existing = this.rule(partId);
    const command = {
      minQuantity: d.minQuantity!,
      maxQuantity: d.maxQuantity ?? null,
      vendorId: d.vendorId || null,
      autoReorderEnabled: d.autoReorderEnabled,
    };
    const result = existing
      ? await this.store.updateStockPolicy(existing.id, command)
      : await this.store.createStockPolicy({ ...command, partId });
    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That threshold could not be saved.');
      return;
    }
    this.closeAllBands();
  }

  protected async deletePolicy(partId: string): Promise<void> {
    const existing = this.rule(partId);
    if (!existing) return;
    this.busy.set(true);
    const result = await this.store.deleteStockPolicy(existing.id);
    this.busy.set(false);
    if (!result.ok) this.bandError.set(result.error ?? null);
    else this.closeAllBands();
  }

  /* --- purchasing ------------------------------------------------------- */

  protected startOrder(): void {
    this.closeAllBands();
    this.orderDraft.set({ vendorId: this.activeVendors()[0]?.id ?? '', lines: [] });
    this.placingOrder.set(true);
  }

  protected addOrderLine(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const partId = select.value;
    select.value = '';
    if (!partId) return;
    this.orderDraft.update((d) =>
      d.lines.some((l) => l.partId === partId)
        ? d
        : { ...d, lines: [...d.lines, { partId, quantity: 1 }] },
    );
  }

  protected changeOrderQuantity(index: number, delta: number): void {
    this.orderDraft.update((d) => ({
      ...d,
      lines: d.lines.map((l, i) => (i === index ? { ...l, quantity: Math.max(1, l.quantity + delta) } : l)),
    }));
  }

  protected removeOrderLine(index: number): void {
    this.orderDraft.update((d) => ({ ...d, lines: d.lines.filter((_, i) => i !== index) }));
  }

  /** What the shortfall would be covered by: the reorder target, when there is one. */
  protected suggestedQuantity(partId: string): number | null {
    const rule = this.rule(partId);
    const part = this.store.part(partId);
    if (!rule?.max || !part) return null;
    return Math.max(0, rule.max - part.available);
  }

  protected async placeOrder(): Promise<void> {
    if (this.of().invalid()) return;
    this.busy.set(true);
    this.bandError.set(null);
    const d = this.orderDraft();
    const result = await this.store.placePurchaseOrder({ vendorId: d.vendorId, lines: d.lines });
    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That order could not be placed.');
      return;
    }
    this.closeAllBands();
  }

  protected openReceipt(po: PurchaseOrder): void {
    this.closeAllBands();
    const draft: Record<string, { quantity: number; unitCost: number }> = {};
    for (const line of po.lines) {
      const outstanding = Math.max(0, line.quantity - line.received);
      // Pre-filled with what is still owed and what it was ordered at: the
      // common case is the whole line arriving at the quoted price.
      draft[line.id] = { quantity: outstanding, unitCost: line.unitCost };
    }
    this.receipt.set(draft);
    this.receiving.set(po.id);
  }

  protected setReceiptQuantity(lineId: string, value: string): void {
    const quantity = Number(value);
    this.receipt.update((r) => ({ ...r, [lineId]: { ...r[lineId], quantity: Number.isFinite(quantity) ? quantity : 0 } }));
  }

  protected setReceiptCost(lineId: string, value: string): void {
    const unitCost = Number(value);
    this.receipt.update((r) => ({ ...r, [lineId]: { ...r[lineId], unitCost: Number.isFinite(unitCost) ? unitCost : 0 } }));
  }

  /** Only lines with something on them are sent; the API refuses an empty receipt. */
  protected readonly receiptLines = computed(() =>
    Object.entries(this.receipt())
      .filter(([, v]) => v.quantity > 0)
      .map(([lineId, v]) => ({ lineId, quantityReceived: v.quantity, unitCost: v.unitCost })),
  );

  protected async recordReceipt(po: PurchaseOrder): Promise<void> {
    const lines = this.receiptLines();
    if (lines.length === 0) return;
    this.busy.set(true);
    this.bandError.set(null);
    const result = await this.store.receivePurchaseOrder(po.id, { lines });
    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That receipt was refused.');
      return;
    }
    this.closeAllBands();
  }

  protected async cancelOrder(po: PurchaseOrder): Promise<void> {
    this.busy.set(true);
    const result = await this.store.cancelPurchaseOrder(po.id);
    this.busy.set(false);
    if (!result.ok) this.bandError.set(result.error ?? null);
    else this.closeAllBands();
  }

  protected outstanding(po: PurchaseOrder): number {
    return po.lines.reduce((sum, l) => sum + Math.max(0, l.quantity - l.received), 0);
  }

  protected canReceive(po: PurchaseOrder): boolean {
    return po.status === 'PLACED' || po.status === 'PARTIALLY_RECEIVED';
  }
}
