import { Component, computed, inject, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import {
  FormField,
  disabled,
  email as emailRule,
  form,
  maxLength,
  minLength,
  required,
  schema,
  validate,
} from '@angular/forms/signals';
import { ShopStore } from '../../core/data/shop-store';
import { Session } from '../../core/auth/session';
import { Callout } from '../../shared/ui/callout';
import { EntryBand } from '../../shared/ui/entry-band';
import { FormFieldRow } from '../../shared/ui/form-field';
import type { Customer } from '../../core/domain/models';
import type { DocumentType, PhoneType } from '../../core/domain/enums';
import { digitsOnly, formatDocument, formatPhone, isValidDocument } from '../../core/domain/documents';

interface CustomerDraft {
  /**
   * Whether this draft is a new record. It lives on the model because Signal
   * Forms owns `disabled` and reads it from the schema — the identity fields are
   * writable exactly once, at creation, and the form itself has to know that.
   */
  creating: boolean;
  firstName: string;
  lastName: string;
  email: string;
  documentType: DocumentType;
  documentCode: string;
  phoneType: PhoneType;
  phone: string;
}

const EMPTY: CustomerDraft = {
  creating: true,
  firstName: '',
  lastName: '',
  email: '',
  documentType: 'CPF',
  documentCode: '',
  phoneType: 'MOBILE',
  phone: '',
};

/**
 * The document is checked here, not only at the API.
 *
 * A CPF's last two digits are a checksum over the first nine, so a mistyped
 * digit is detectable without a round trip — and the operator is usually still
 * looking at the card they copied it from. The API agrees (422, "Invalid CPF
 * document code"); catching it in the field just means nobody has to wait to
 * learn it.
 */
const customerSchema = schema<CustomerDraft>((p) => {
  required(p.firstName, { message: 'A first name is required.' });
  minLength(p.firstName, 3, { message: 'At least 3 characters.' });
  maxLength(p.firstName, 30, { message: 'At most 30 characters.' });
  maxLength(p.lastName, 30, { message: 'At most 30 characters.' });
  required(p.email, { message: 'An email is required.' });
  emailRule(p.email, { message: 'That is not a valid email address.' });
  required(p.documentCode, { message: 'A document is required.' });
  validate(p.documentCode, (ctx) => {
    const type = ctx.valueOf(p.documentType);
    const code = ctx.value();
    if (!code.trim()) return null;
    return isValidDocument(type, code)
      ? null
      : { kind: 'document', message: `That is not a valid ${type} — check the digits.` };
  });
  // The API refuses to change an email or a document after the fact, so they are
  // shown but sealed on an edit rather than offered as blanks that would be lost.
  disabled(p.email, { when: (ctx) => !ctx.valueOf(p.creating) });
  disabled(p.documentType, { when: (ctx) => !ctx.valueOf(p.creating) });
  disabled(p.documentCode, { when: (ctx) => !ctx.valueOf(p.creating) });
  required(p.phone, { message: 'At least one phone number is required.' });
  validate(p.phone, (ctx) => {
    const d = digitsOnly(ctx.value());
    if (!d) return null;
    return d.length === 10 || d.length === 11
      ? null
      : { kind: 'phone', message: 'A Brazilian number has 10 or 11 digits including the area code.' };
  });
});

@Component({
  selector: 'app-customers',
  imports: [Callout, EntryBand, FormFieldRow, FormField, NgTemplateOutlet],
  templateUrl: './customers.html',
  styleUrl: './records.scss',
})
export class Customers {
  protected readonly store = inject(ShopStore);
  private readonly session = inject(Session);

  protected readonly q = signal('');

  /** `new` for a blank line at the head of the table, or the id being edited. */
  protected readonly open = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly bandError = signal<string | null>(null);
  /** The id whose deactivation is awaiting confirmation, inline. */
  protected readonly confirming = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly draft = signal<CustomerDraft>({ ...EMPTY });
  protected readonly f = form(this.draft, customerSchema);

  protected readonly canWrite = computed(() => this.session.hasAnyRole('ATTENDANT', 'MANAGER'));
  protected readonly isDemo = this.store.isDemo;

  protected readonly rows = computed(() => {
    const q = this.q().trim().toLowerCase();
    const bare = digitsOnly(q);
    return this.store
      .customers()
      .filter((c) =>
        q
          ? [c.name, c.email].some((v) => v.toLowerCase().includes(q)) ||
            (bare.length > 0 && digitsOnly(c.document).includes(bare))
          : true,
      )
      .sort((a, b) => Number(b.active) - Number(a.active) || a.name.localeCompare(b.name));
  });

  protected readonly deactivated = computed(() => this.store.customers().filter((c) => !c.active));

  protected vehicleCount(customerId: string): number {
    return this.store.vehicleCounts().get(customerId) ?? 0;
  }

  protected doc(c: Customer): string {
    return formatDocument(c.documentType, c.document);
  }

  protected phone(c: Customer): string {
    return c.phone ? formatPhone(c.phone) : '—';
  }

  protected onQuery(event: Event): void {
    this.q.set((event.target as HTMLInputElement).value);
  }

  /* --- the band --------------------------------------------------------- */

  protected openNew(): void {
    this.draft.set({ ...EMPTY });
    this.bandError.set(null);
    this.confirming.set(null);
    this.open.set('new');
  }

  /**
   * A profile edit carries the name and phone only — the API refuses to change
   * an email or a document after the fact, so neither is offered as a blank the
   * operator could fill in and then lose.
   */
  protected openEdit(c: Customer): void {
    this.draft.set({
      creating: false,
      firstName: c.name.split(' ')[0] ?? '',
      lastName: c.name.split(' ').slice(1).join(' '),
      email: c.email,
      documentType: c.documentType,
      documentCode: c.document,
      phoneType: 'MOBILE',
      phone: c.phone ?? '',
    });
    this.bandError.set(null);
    this.confirming.set(null);
    this.open.set(c.id);
  }

  protected close(): void {
    this.open.set(null);
    this.bandError.set(null);
  }

  protected async save(): Promise<void> {
    const target = this.open();
    if (!target || this.f().invalid()) return;

    this.busy.set(true);
    this.bandError.set(null);
    const d = this.draft();
    const phones = [{ type: d.phoneType, phone: digitsOnly(d.phone), isPrimary: true }];

    const result =
      target === 'new'
        ? await this.store.createCustomer({
            email: d.email.trim(),
            firstName: d.firstName.trim(),
            lastName: d.lastName.trim(),
            documentType: d.documentType,
            documentCode: digitsOnly(d.documentCode),
            phoneNumbers: phones,
          })
        : await this.store.updateCustomer(target, {
            firstName: d.firstName.trim(),
            lastName: d.lastName.trim(),
            phoneNumbers: phones,
          });

    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That record could not be saved.');
      return;
    }

    // Worth saying once: a staff-created customer has no password yet.
    this.notice.set(
      target === 'new'
        ? `${d.firstName} ${d.lastName}`.trim() +
            ' is on file. They have no sign-in yet — they set a password through “forgot password” on the customer site.'
        : null,
    );
    this.close();
  }

  protected async setActive(c: Customer, active: boolean): Promise<void> {
    this.busy.set(true);
    const result = await this.store.setCustomerActive(c.id, active);
    this.busy.set(false);
    this.confirming.set(null);
    if (!result.ok) this.notice.set(result.error ?? null);
  }

  protected dismissNotice(): void {
    this.notice.set(null);
  }
}
