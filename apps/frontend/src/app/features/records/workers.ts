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
import { WORKER_ROLE_LABEL, type DocumentType, type PhoneType, type WorkerRole } from '../../core/domain/enums';
import type { Worker } from '../../core/domain/models';
import { Callout } from '../../shared/ui/callout';
import { EntryBand } from '../../shared/ui/entry-band';
import { FormFieldRow } from '../../shared/ui/form-field';
import { digitsOnly, isValidDocument } from '../../core/domain/documents';

interface WorkerDraft {
  creating: boolean;
  firstName: string;
  lastName: string;
  email: string;
  documentType: DocumentType;
  documentCode: string;
  phoneType: PhoneType;
  phone: string;
  role: WorkerRole;
  hireDate: string;
  startDate: string;
  password: string;
}

const today = () => new Date().toISOString().slice(0, 10);

const EMPTY: WorkerDraft = {
  creating: true,
  firstName: '',
  lastName: '',
  email: '',
  documentType: 'CPF',
  documentCode: '',
  phoneType: 'MOBILE',
  phone: '',
  role: 'MECHANIC',
  hireDate: today(),
  startDate: today(),
  password: '',
};

const workerSchema = schema<WorkerDraft>((p) => {
  required(p.firstName, { message: 'A first name is required.' });
  minLength(p.firstName, 3, { message: 'At least 3 characters.' });
  maxLength(p.firstName, 30, { message: 'At most 30 characters.' });
  maxLength(p.lastName, 30, { message: 'At most 30 characters.' });
  required(p.email, { message: 'An email is required.' });
  emailRule(p.email, { message: 'That is not a valid email address.' });
  required(p.documentCode, { message: 'A document is required.' });
  validate(p.documentCode, (ctx) => {
    const code = ctx.value();
    if (!code.trim()) return null;
    return isValidDocument(ctx.valueOf(p.documentType), code)
      ? null
      : { kind: 'document', message: `That is not a valid ${ctx.valueOf(p.documentType)}.` };
  });
  required(p.phone, { message: 'At least one phone number is required.' });
  validate(p.phone, (ctx) => {
    const d = digitsOnly(ctx.value());
    if (!d) return null;
    return d.length === 10 || d.length === 11
      ? null
      : { kind: 'phone', message: 'A Brazilian number has 10 or 11 digits.' };
  });
  required(p.hireDate, { message: 'A hire date is required.' });
  required(p.startDate, { message: 'A start date is required.' });
  // The backend refuses a start before the hire, with its own exception.
  validate(p.startDate, (ctx) => {
    const start = ctx.value();
    const hire = ctx.valueOf(p.hireDate);
    if (!start || !hire) return null;
    return start >= hire ? null : { kind: 'dates', message: 'A worker cannot start before being hired.' };
  });

  // A worker is given a password at creation — they must be able to sign in.
  required(p.password, { message: 'A first password is required.' });
  minLength(p.password, 16, { message: 'At least 16 characters — this is a staff console.' });
  maxLength(p.password, 72, { message: 'At most 72 characters.' });

  // Identity and terms of employment are fixed once the record exists.
  disabled(p.email, { when: (ctx) => !ctx.valueOf(p.creating) });
  disabled(p.documentType, { when: (ctx) => !ctx.valueOf(p.creating) });
  disabled(p.documentCode, { when: (ctx) => !ctx.valueOf(p.creating) });
  disabled(p.role, { when: (ctx) => !ctx.valueOf(p.creating) });
  disabled(p.hireDate, { when: (ctx) => !ctx.valueOf(p.creating) });
  disabled(p.startDate, { when: (ctx) => !ctx.valueOf(p.creating) });
  disabled(p.password, { when: (ctx) => !ctx.valueOf(p.creating) });
});

@Component({
  selector: 'app-workers',
  imports: [Callout, EntryBand, FormFieldRow, FormField, NgTemplateOutlet],
  templateUrl: './workers.html',
  styleUrl: './records.scss',
})
export class Workers {
  protected readonly store = inject(ShopStore);
  private readonly session = inject(Session);

  protected readonly open = signal<string | null>(null);
  protected readonly terminating = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly bandError = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly draft = signal<WorkerDraft>({ ...EMPTY });
  protected readonly f = form(this.draft, workerSchema);

  protected readonly canWrite = computed(() => this.session.hasAnyRole('MANAGER'));
  protected readonly isDemo = this.store.isDemo;

  protected readonly rows = computed(() =>
    [...this.store.workers()].sort(
      (a, b) => Number(b.active) - Number(a.active) || a.name.localeCompare(b.name),
    ),
  );

  protected readonly active = computed(() => this.store.workers().filter((w) => w.active));
  protected readonly terminated = computed(() => this.store.workers().filter((w) => !w.active));

  protected readonly roles = Object.entries(WORKER_ROLE_LABEL) as [WorkerRole, string][];

  protected roleLabel(role: WorkerRole): string {
    return WORKER_ROLE_LABEL[role];
  }

  /** Terminating yourself would end the session you are working in. */
  protected isSelf(w: Worker): boolean {
    return this.session.worker()?.id === w.id;
  }

  protected openNew(): void {
    this.draft.set({ ...EMPTY });
    this.bandError.set(null);
    this.terminating.set(null);
    this.open.set('new');
  }

  protected openEdit(w: Worker): void {
    this.draft.set({
      ...EMPTY,
      creating: false,
      firstName: w.name.split(' ')[0] ?? '',
      lastName: w.name.split(' ').slice(1).join(' '),
      email: w.email,
      role: w.role,
      hireDate: w.hiredAt ?? today(),
      startDate: w.hiredAt ?? today(),
      phone: w.phone ?? '',
      // Sealed on an edit and therefore not validated, but the schema still
      // declares them, so they carry placeholders the API will never see.
      documentCode: '00000000000',
      password: 'x'.repeat(16),
    });
    this.bandError.set(null);
    this.terminating.set(null);
    this.open.set(w.id);
  }

  protected close(): void {
    this.open.set(null);
    this.bandError.set(null);
  }

  /** A password nobody has to invent, and long enough to satisfy the rule. */
  protected generatePassword(): void {
    const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';
    const bytes = crypto.getRandomValues(new Uint32Array(20));
    const password = [...bytes].map((n) => alphabet[n % alphabet.length]).join('');
    this.draft.update((d) => ({ ...d, password }));
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
        ? await this.store.registerWorker(
            {
              user: {
                email: d.email.trim(),
                firstName: d.firstName.trim(),
                lastName: d.lastName.trim(),
                documentType: d.documentType,
                documentCode: digitsOnly(d.documentCode),
                phoneNumbers: phones,
              },
              role: d.role,
              hireDate: d.hireDate,
              startDate: d.startDate,
            },
            d.password,
          )
        : await this.store.updateWorker(target, {
            firstName: d.firstName.trim(),
            lastName: d.lastName.trim(),
            phoneNumbers: phones,
          });

    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That worker could not be saved.');
      return;
    }

    if (target === 'new') {
      this.notice.set(
        `${d.firstName} ${d.lastName}`.trim() +
          ' can sign in with the password you set. Hand it over now — it is not stored anywhere you can read it back.',
      );
    }
    this.close();
  }

  protected async terminate(w: Worker): Promise<void> {
    this.busy.set(true);
    const result = await this.store.terminateWorker(w.id);
    this.busy.set(false);
    this.terminating.set(null);
    if (!result.ok) this.notice.set(result.error ?? null);
  }

  protected dismissNotice(): void {
    this.notice.set(null);
  }
}
