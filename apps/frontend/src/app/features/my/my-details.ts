import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/api/api-client';
import { ShopApi } from '../../core/api/shop-api';
import { Session } from '../../core/auth/session';
import { formatDocument, formatPhone, normalisePhone } from '../../core/domain/documents';
import { PHONE_TYPE_LABEL } from '../../core/domain/enums';
import type { PhoneType } from '../../core/domain/enums';
import { Callout } from '../../shared/ui/callout';

/**
 * The account itself.
 *
 * `PATCH /customers/{id}` accepts a name and phone numbers and nothing else —
 * the email and the document are deliberately immutable there, and changing an
 * email is its own confirmed flow (`POST /auth/email-change`) because it moves
 * where every future budget is sent. So those two are printed as facts with
 * the route to change them named, rather than rendered as blanks that would
 * silently do nothing.
 *
 * The whole command is sent on every save: `UpdateUserProfileCommand` requires
 * at least one phone number, so a partial patch that dropped the list would be
 * refused. The form therefore always carries a full, valid list.
 */
@Component({
  selector: 'app-my-details',
  imports: [Callout, RouterLink],
  templateUrl: './my-details.html',
  styleUrls: ['./garage.scss', './my-details.scss'],
})
export class MyDetails {
  private readonly api = inject(ShopApi);
  protected readonly session = inject(Session);

  protected readonly phoneTypes: readonly PhoneType[] = ['MOBILE', 'COMMERCIAL', 'HOME', 'OTHER'];
  protected readonly phoneLabel = PHONE_TYPE_LABEL;

  protected readonly user = this.session.user;

  protected readonly editing = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly saved = signal(false);

  protected readonly firstName = signal('');
  protected readonly lastName = signal('');
  protected readonly phoneType = signal<PhoneType>('MOBILE');
  protected readonly phone = signal('');

  /* --- the email change, its own flow ------------------------------------- */

  protected readonly changingEmail = signal(false);
  protected readonly newEmail = signal('');
  protected readonly emailBusy = signal(false);
  protected readonly emailError = signal<string | null>(null);
  protected readonly emailSent = signal(false);

  protected readonly primaryPhone = computed(() => {
    const phones = this.user()?.phoneNumbers ?? [];
    return phones.find((p) => p.isPrimary) ?? phones[0] ?? null;
  });

  protected readonly nameError = computed(() => {
    const value = this.firstName().trim();
    if (value.length < 3) return 'At least 3 characters.';
    if (value.length > 30) return 'At most 30 characters.';
    return null;
  });

  protected readonly phoneError = computed(() => {
    const digits = normalisePhone(this.phone());
    if (digits.length < 10) return 'At least 10 digits, with the area code.';
    return null;
  });

  protected readonly canSave = computed(
    () => this.nameError() === null && this.phoneError() === null,
  );

  protected edit(): void {
    const user = this.user();
    if (!user) return;
    this.firstName.set(user.firstName);
    this.lastName.set(user.lastName ?? '');
    const primary = this.primaryPhone();
    this.phoneType.set(primary?.type ?? 'MOBILE');
    this.phone.set(primary ? formatPhone(primary.phone) : '');
    this.error.set(null);
    this.saved.set(false);
    this.editing.set(true);
  }

  protected async save(): Promise<void> {
    const user = this.user();
    if (!user || !this.canSave() || this.busy()) return;

    this.busy.set(true);
    this.error.set(null);
    try {
      await this.api.updateCustomer(user.id, {
        firstName: this.firstName().trim(),
        lastName: this.lastName().trim(),
        phoneNumbers: [
          { type: this.phoneType(), phone: normalisePhone(this.phone()), isPrimary: true },
        ],
      });
      await this.session.refreshUser();
      this.editing.set(false);
      this.saved.set(true);
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'Your details were not saved.');
    } finally {
      this.busy.set(false);
    }
  }

  protected async requestEmailChange(): Promise<void> {
    const value = this.newEmail().trim();
    if (!value || this.emailBusy()) return;
    this.emailBusy.set(true);
    this.emailError.set(null);
    try {
      await this.api.requestEmailChange(value);
      this.emailSent.set(true);
      this.changingEmail.set(false);
      this.newEmail.set('');
    } catch (error) {
      this.emailError.set(
        error instanceof ApiError ? error.message : 'The change could not be started.',
      );
    } finally {
      this.emailBusy.set(false);
    }
  }

  protected document(): string {
    const user = this.user();
    if (!user) return '—';
    return formatDocument(user.documentType, user.documentCode);
  }

  protected shownPhone(): string {
    const primary = this.primaryPhone();
    return primary ? formatPhone(primary.phone) : '—';
  }
}
