import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/api/api-client';
import { ShopApi } from '../../core/api/shop-api';
import type { AppointmentInfoDto } from '../../core/api/dto';
import { digitsOnly, isValidDocument, isValidPlate } from '../../core/domain/documents';
import {
  DOCUMENT_TYPE_LABEL,
  FUEL_TYPE_LABEL,
  TRANSMISSION_LABEL,
  VEHICLE_TYPE_LABEL,
} from '../../core/domain/enums';
import type {
  DocumentType,
  FuelType,
  TransmissionType,
  VehicleType,
} from '../../core/domain/enums';
import { Callout } from '../../shared/ui/callout';
import { Icon } from '../../shared/ui/icon';

/**
 * `/appointments/complete-registration?token=…` — the guest becomes a customer.
 *
 * The shop already knows their name, email, phone and roughly what they drive:
 * they typed all of it into the landing card. So this form asks only for what
 * the guest card deliberately left out — a password, the document a `User`
 * requires, and the vehicle fields `CreateVehicleCommand` needs beyond
 * make/model/year. What is already known is named rather than re-asked;
 * re-typing your own name to claim an account you were invited to is busywork.
 *
 * ## The single-use token
 *
 * `consumeRegistrationToken` marks the token used. There is no read-only
 * "check this token" call, so this page cannot verify the link before showing
 * the form — the token is spent by the one request that also creates the
 * account. Two consequences the design has to carry:
 *
 * - The form renders optimistically. An expired or already-spent link fails on
 *   submit, not on arrival, so that failure state has to explain itself
 *   properly rather than reading as a validation error.
 * - Converting on a second device fails, because the first device spent it.
 *   That is the likeliest failure here and its message says exactly that.
 */
@Component({
  selector: 'app-complete-registration',
  imports: [Callout, Icon, RouterLink],
  templateUrl: './complete-registration.html',
  styleUrls: ['./guest.scss', './complete-registration.scss'],
})
export class CompleteRegistration {
  private readonly api = inject(ShopApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly token = this.route.snapshot.queryParamMap.get('token');

  protected readonly documentTypes: readonly DocumentType[] = ['CPF', 'CNPJ'];
  protected readonly vehicleTypes: readonly VehicleType[] = [
    'CAR',
    'SUV',
    'MOTORCYCLE',
    'VAN',
    'TRUCK',
    'BUS',
    'OTHER',
  ];
  protected readonly fuelTypes: readonly FuelType[] = [
    'FLEX',
    'GASOLINE',
    'ETHANOL',
    'DIESEL',
    'ELECTRIC',
    'HYBRID',
    'GNV',
    'OTHER',
  ];
  protected readonly transmissions: readonly TransmissionType[] = [
    'MANUAL',
    'AUTOMATIC',
    'CVT',
    'AUTOMATED_MANUAL',
  ];

  protected readonly documentLabel = DOCUMENT_TYPE_LABEL;
  protected readonly typeLabel = VEHICLE_TYPE_LABEL;
  protected readonly fuelLabel = FUEL_TYPE_LABEL;
  protected readonly transmissionLabel = TRANSMISSION_LABEL;

  /**
   * Only ever set from the conversion response, never read up front.
   *
   * There is no read-by-registration-token endpoint, and the email puts the
   * access token on a *different* link — so this page genuinely cannot see the
   * booking before the account is made. It therefore states what the shop
   * already holds rather than listing it back, which is true either way.
   */
  protected readonly booking = signal<AppointmentInfoDto | null>(null);

  protected readonly documentType = signal<DocumentType>('CPF');
  protected readonly documentCode = signal('');
  protected readonly plate = signal('');
  protected readonly colour = signal('');
  protected readonly vehicleType = signal<VehicleType>('CAR');
  protected readonly fuelType = signal<FuelType>('FLEX');
  protected readonly transmission = signal<TransmissionType>('MANUAL');
  protected readonly password = signal('');
  protected readonly confirm = signal('');
  protected readonly revealed = signal(false);

  protected readonly touched = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly done = signal(false);
  protected readonly spent = signal(false);

  protected readonly missingToken = !this.token;

  protected readonly documentError = computed(() => {
    const value = this.documentCode().trim();
    if (value.length === 0) return `A ${this.documentType()} is required.`;
    if (!isValidDocument(this.documentType(), value)) {
      return `That ${this.documentType()} does not check out.`;
    }
    return null;
  });

  protected readonly plateError = computed(() => {
    const value = this.plate().trim();
    if (value.length === 0) return 'A plate is required.';
    if (!isValidPlate(value)) return 'Not a Brazilian plate — ABC1D23 or ABC1234.';
    return null;
  });

  protected readonly colourError = computed(() =>
    this.colour().trim().length === 0 ? 'A colour is required.' : null,
  );

  protected readonly passwordError = computed(() => {
    const value = this.password();
    if (value.length === 0) return 'A password is required.';
    if (value.length < 16) {
      const left = 16 - value.length;
      return `${left} more character${left === 1 ? '' : 's'} to go.`;
    }
    if (value.length > 72) return 'At most 72 characters.';
    return null;
  });

  protected readonly confirmError = computed(() =>
    this.confirm().length > 0 && this.confirm() !== this.password()
      ? 'The two do not match.'
      : null,
  );

  protected readonly passwordProgress = computed(() =>
    Math.min(100, Math.round((this.password().length / 16) * 100)),
  );

  protected readonly complete = computed(
    () =>
      this.documentError() === null &&
      this.plateError() === null &&
      this.colourError() === null &&
      this.passwordError() === null &&
      this.confirm() === this.password(),
  );

  protected shown(message: string | null): string | null {
    return this.touched() ? message : null;
  }

  protected async submit(event: Event): Promise<void> {
    event.preventDefault();
    this.touched.set(true);
    if (!this.token || !this.complete() || this.busy()) return;

    this.busy.set(true);
    this.error.set(null);
    try {
      const result = await this.api.completeGuestRegistration({
        token: this.token,
        rawPassword: this.password(),
        documentType: this.documentType(),
        documentCode: digitsOnly(this.documentCode()),
        licensePlate: this.plate().trim().toUpperCase().replace(/[^A-Z0-9]/g, ''),
        vehicleType: this.vehicleType(),
        color: this.colour().trim(),
        fuelType: this.fuelType(),
        transmissionType: this.transmission(),
      });
      this.booking.set(result.appointment);
      this.done.set(true);
    } catch (error) {
      // A spent or expired token is the likeliest failure on this page — the
      // customer converted on another device, or left it a fortnight. It reads
      // as its own state rather than as a field-level complaint.
      if (error instanceof ApiError && (error.status === 400 || error.status === 404)) {
        this.spent.set(true);
      } else {
        this.error.set(
          error instanceof ApiError ? error.message : 'The account could not be created.',
        );
      }
    } finally {
      this.password.set('');
      this.confirm.set('');
      this.busy.set(false);
    }
  }

  /**
   * Conversion returns no session, so the customer signs in — but with an
   * address the backend now marks verified, because consuming a token mailed
   * to it is the same proof email verification asks for. The email is carried
   * over so they are typing one thing, not two.
   */
  protected async goToSignIn(): Promise<void> {
    await this.router.navigate(['/sign-in'], {
      queryParams: { next: '/my' },
      state: { email: this.booking()?.guestEmail ?? '' },
    });
  }

  protected when(instant: string): string {
    return new Date(instant).toLocaleString('en-GB', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

}
