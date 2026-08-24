import { Component, computed, inject, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import {
  FormField,
  disabled,
  form,
  max,
  maxLength,
  min,
  required,
  schema,
  validate,
} from '@angular/forms/signals';
import { ShopStore } from '../../core/data/shop-store';
import { Session } from '../../core/auth/session';
import { EntryBand } from '../../shared/ui/entry-band';
import { FormFieldRow } from '../../shared/ui/form-field';
import { Callout } from '../../shared/ui/callout';
import {
  FUEL_TYPE_LABEL,
  TRANSMISSION_LABEL,
  VEHICLE_TYPE_LABEL,
  type FuelType,
  type TransmissionType,
  type VehicleType,
} from '../../core/domain/enums';
import type { Vehicle } from '../../core/domain/models';
import { isValidPlate } from '../../core/domain/documents';

interface VehicleDraft {
  creating: boolean;
  customerId: string;
  licensePlate: string;
  make: string;
  model: string;
  version: string;
  color: string;
  vehicleType: VehicleType;
  modelYear: number | null;
  manufactureYear: number | null;
  fuelType: FuelType;
  transmissionType: TransmissionType;
}

const EMPTY: VehicleDraft = {
  creating: true,
  customerId: '',
  licensePlate: '',
  make: '',
  model: '',
  version: '',
  color: '',
  vehicleType: 'CAR',
  modelYear: null,
  manufactureYear: null,
  fuelType: 'FLEX',
  transmissionType: 'MANUAL',
};

/** A model year one ahead of the calendar is normal in the trade; two is a typo. */
const NEXT_YEAR = new Date().getFullYear() + 1;

const vehicleSchema = schema<VehicleDraft>((p) => {
  required(p.customerId, { message: 'A vehicle belongs to a customer — pick the owner.' });
  required(p.licensePlate, { message: 'A plate is required.' });
  maxLength(p.licensePlate, 7, { message: 'A plate is 7 characters.' });
  validate(p.licensePlate, (ctx) => {
    const v = ctx.value().trim();
    if (!v) return null;
    return isValidPlate(v)
      ? null
      : { kind: 'plate', message: 'Expected ABC1D23 (Mercosul) or ABC1234.' };
  });
  required(p.make, { message: 'A make is required.' });
  maxLength(p.make, 50);
  required(p.model, { message: 'A model is required.' });
  maxLength(p.model, 100);
  maxLength(p.version, 100);
  required(p.color, { message: 'A colour is required.' });
  maxLength(p.color, 30, { message: 'At most 30 characters.' });
  required(p.modelYear, { message: 'A model year is required.' });
  min(p.modelYear, 1900, { message: 'That is before the motor car.' });
  max(p.modelYear, NEXT_YEAR, { message: `No later than ${NEXT_YEAR}.` });
  min(p.manufactureYear, 1900, { message: 'That is before the motor car.' });
  max(p.manufactureYear, NEXT_YEAR, { message: `No later than ${NEXT_YEAR}.` });
  // A car is built before or in its model year, never after it.
  validate(p.manufactureYear, (ctx) => {
    const made = ctx.value();
    const model = ctx.valueOf(p.modelYear);
    if (made === null || model === null) return null;
    return made <= model
      ? null
      : { kind: 'years', message: 'A vehicle is built before or in its model year.' };
  });
  // The API binds a vehicle to its owner at creation and will not move it after.
  disabled(p.customerId, { when: (ctx) => !ctx.valueOf(p.creating) });
});

@Component({
  selector: 'app-vehicles',
  imports: [EntryBand, FormFieldRow, FormField, NgTemplateOutlet, Callout],
  templateUrl: './vehicles.html',
  styleUrl: './records.scss',
})
export class Vehicles {
  protected readonly store = inject(ShopStore);
  private readonly session = inject(Session);

  protected readonly q = signal('');
  protected readonly open = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly bandError = signal<string | null>(null);
  protected readonly confirming = signal<string | null>(null);

  protected readonly draft = signal<VehicleDraft>({ ...EMPTY });
  protected readonly f = form(this.draft, vehicleSchema);

  protected readonly canWrite = computed(() => this.session.hasAnyRole('ATTENDANT', 'MANAGER'));
  protected readonly isDemo = this.store.isDemo;

  /** Only an active customer can take a new vehicle. */
  protected readonly owners = computed(() =>
    this.store
      .customers()
      .filter((c) => c.active)
      .sort((a, b) => a.name.localeCompare(b.name)),
  );

  protected readonly rows = computed(() => {
    const q = this.q().trim().toLowerCase().replace(/[^a-z0-9]/g, '');
    return this.store.vehicles().filter((v) =>
      q
        ? [v.licensePlate, v.make, v.model, this.ownerName(v)]
            .join(' ')
            .toLowerCase()
            .replace(/[^a-z0-9 ]/g, '')
            .includes(q)
        : true,
    );
  });

  protected ownerName(v: Vehicle): string {
    return this.store.customer(v.customerId)?.name ?? '—';
  }

  protected onQuery(event: Event): void {
    this.q.set((event.target as HTMLInputElement).value);
  }

  protected typeLabel(t: keyof typeof VEHICLE_TYPE_LABEL): string {
    return VEHICLE_TYPE_LABEL[t];
  }

  protected fuelLabel(f: keyof typeof FUEL_TYPE_LABEL): string {
    return FUEL_TYPE_LABEL[f];
  }

  protected transmission(t: keyof typeof TRANSMISSION_LABEL): string {
    return TRANSMISSION_LABEL[t];
  }

  protected readonly vehicleTypes = Object.entries(VEHICLE_TYPE_LABEL) as [VehicleType, string][];
  protected readonly fuelTypes = Object.entries(FUEL_TYPE_LABEL) as [FuelType, string][];
  protected readonly transmissions = Object.entries(TRANSMISSION_LABEL) as [
    TransmissionType,
    string,
  ][];

  /* --- the band --------------------------------------------------------- */

  protected openNew(): void {
    this.draft.set({ ...EMPTY, customerId: this.owners()[0]?.id ?? '' });
    this.bandError.set(null);
    this.confirming.set(null);
    this.open.set('new');
  }

  protected openEdit(v: Vehicle): void {
    this.draft.set({
      creating: false,
      customerId: v.customerId,
      licensePlate: v.licensePlate,
      make: v.make,
      model: v.model,
      version: '',
      color: v.color ?? '',
      vehicleType: v.vehicleType,
      modelYear: v.modelYear,
      manufactureYear: v.manufactureYear,
      fuelType: v.fuelType,
      transmissionType: v.transmissionType,
    });
    this.bandError.set(null);
    this.confirming.set(null);
    this.open.set(v.id);
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
    const command = {
      vehicleType: d.vehicleType,
      licensePlate: d.licensePlate.toUpperCase().replace(/[^A-Z0-9]/g, ''),
      make: d.make.trim(),
      model: d.model.trim(),
      color: d.color.trim(),
      modelYear: d.modelYear!,
      manufactureYear: d.manufactureYear ?? d.modelYear!,
      version: d.version.trim() || null,
      fuelType: d.fuelType,
      transmissionType: d.transmissionType,
    };

    const result =
      target === 'new'
        ? await this.store.createVehicle({ ...command, customerId: d.customerId })
        : await this.store.updateVehicle(target, command);

    this.busy.set(false);
    if (!result.ok) {
      this.bandError.set(result.error ?? 'That vehicle could not be saved.');
      return;
    }
    this.close();
  }

  protected async deactivate(v: Vehicle): Promise<void> {
    this.busy.set(true);
    const result = await this.store.deactivateVehicle(v.id);
    this.busy.set(false);
    this.confirming.set(null);
    if (!result.ok) this.bandError.set(result.error ?? null);
  }
}
