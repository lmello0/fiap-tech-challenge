import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Session } from '../../core/auth/session';
import { digitsOnly, isValidDocument, normalisePhone } from '../../core/domain/documents';
import { DOCUMENT_TYPE_LABEL, PHONE_TYPE_LABEL } from '../../core/domain/enums';
import type { DocumentType, PhoneType } from '../../core/domain/enums';
import { Callout } from '../../shared/ui/callout';

/**
 * The other half of the front door: opening an account.
 *
 * Registration creates a **Customer** facet and nothing else — a Worker is
 * created by a manager and never self-served, which is the backend's rule and
 * not a simplification here. `POST /auth/register/customer` answers with the
 * token pair itself, so the account exists and the person is signed into it in
 * one step; there is no "now sign in" screen after this one.
 *
 * The card stays a card: the same ruled blanks and the same form-header band
 * as the drop-off order, because it is the same shop's stationery. What it
 * loses is the routing margin — nothing is being routed yet.
 *
 * Every constraint printed beside a blank is the backend's own: 3–30 for a
 * first name, 16–72 for a password, a CPF or CNPJ that passes its own check
 * digits. They are validated here so a refusal costs a keystroke instead of a
 * round trip, never as a second, looser opinion.
 */
@Component({
  selector: 'app-register',
  imports: [Callout, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly router = inject(Router);
  protected readonly session = inject(Session);

  protected readonly documentTypes: readonly DocumentType[] = ['CPF', 'CNPJ'];
  protected readonly phoneTypes: readonly PhoneType[] = ['MOBILE', 'COMMERCIAL', 'HOME', 'OTHER'];
  protected readonly documentLabel = DOCUMENT_TYPE_LABEL;
  protected readonly phoneLabel = PHONE_TYPE_LABEL;

  protected readonly firstName = signal('');
  protected readonly lastName = signal('');
  protected readonly email = signal('');
  protected readonly documentType = signal<DocumentType>('CPF');
  protected readonly documentCode = signal('');
  protected readonly phoneType = signal<PhoneType>('MOBILE');
  protected readonly phone = signal('');
  protected readonly password = signal('');
  protected readonly confirm = signal('');

  protected readonly touched = signal(false);
  protected readonly revealed = signal(false);

  constructor() {
    // The landing card hands over what it already collected. Only what
    // registration can genuinely use travels — the vehicle and the complaint
    // are not part of this command, and carrying them would drop them here.
    const carried = this.router.getCurrentNavigation()?.extras.state as
      | { name?: string; phone?: string; email?: string }
      | undefined;
    if (carried?.email) this.email.set(carried.email);
    if (carried?.phone) this.phone.set(carried.phone);
    if (carried?.name) {
      const parts = carried.name.trim().split(/\s+/);
      this.firstName.set(parts[0] ?? '');
      this.lastName.set(parts.slice(1).join(' ').slice(0, 30));
    }
  }

  /* --- what each blank will and will not accept --------------------------- */

  protected readonly firstNameError = computed(() => {
    const value = this.firstName().trim();
    if (value.length === 0) return 'A first name is required.';
    if (value.length < 3) return 'At least 3 characters.';
    if (value.length > 30) return 'At most 30 characters.';
    return null;
  });

  protected readonly lastNameError = computed(() =>
    this.lastName().trim().length > 30 ? 'At most 30 characters.' : null,
  );

  protected readonly emailError = computed(() => {
    const value = this.email().trim();
    if (value.length === 0) return 'An email is required.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return 'That is not an email address.';
    return null;
  });

  protected readonly documentError = computed(() => {
    const value = this.documentCode().trim();
    if (value.length === 0) return `A ${this.documentType()} is required.`;
    if (!isValidDocument(this.documentType(), value)) {
      return `That ${this.documentType()} does not check out.`;
    }
    return null;
  });

  protected readonly phoneError = computed(() => {
    const digits = normalisePhone(this.phone());
    if (digits.length === 0) return 'At least one phone number is required.';
    if (digits.length < 10) return 'That looks too short to be a phone number.';
    return null;
  });

  protected readonly passwordError = computed(() => {
    const value = this.password();
    if (value.length === 0) return 'A password is required.';
    if (value.length < 16) return `${16 - value.length} more character${16 - value.length === 1 ? '' : 's'} to go.`;
    if (value.length > 72) return 'At most 72 characters.';
    return null;
  });

  protected readonly confirmError = computed(() =>
    this.confirm().length > 0 && this.confirm() !== this.password()
      ? 'The two do not match.'
      : null,
  );

  protected readonly complete = computed(
    () =>
      this.firstNameError() === null &&
      this.lastNameError() === null &&
      this.emailError() === null &&
      this.documentError() === null &&
      this.phoneError() === null &&
      this.passwordError() === null &&
      this.confirm() === this.password(),
  );

  /** A 16-character floor is unusual enough to be worth showing progress against. */
  protected readonly passwordProgress = computed(() =>
    Math.min(100, Math.round((this.password().length / 16) * 100)),
  );

  /** Errors stay quiet until the form has been submitted once. A form that
   *  scolds every blank before it has been filled is arguing with the person. */
  protected shown(message: string | null): string | null {
    return this.touched() ? message : null;
  }

  protected async submit(event: Event): Promise<void> {
    event.preventDefault();
    this.touched.set(true);
    if (!this.complete() || this.session.busy()) return;

    const outcome = await this.session.register({
      user: {
        email: this.email().trim(),
        firstName: this.firstName().trim(),
        lastName: this.lastName().trim(),
        documentType: this.documentType(),
        documentCode: digitsOnly(this.documentCode()),
        phoneNumbers: [
          { type: this.phoneType(), phone: normalisePhone(this.phone()), isPrimary: true },
        ],
      },
      rawPassword: this.password(),
    });

    this.password.set('');
    this.confirm.set('');

    // Registration can only ever produce a Customer facet, so the picker is
    // never the answer here — but route on the outcome rather than assuming it,
    // so an account that somehow already held a Worker facet still lands right.
    if (outcome.kind === 'facet') {
      await this.router.navigate([outcome.facet === 'worker' ? '/work-orders' : '/my']);
    } else if (outcome.kind === 'choose') {
      await this.router.navigate(['/choose']);
    }
  }
}
