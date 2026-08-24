import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ApiError } from '../../core/api/api-client';
import type { VehicleCommand } from '../../core/api/shop-api';
import { CustomerStore } from '../../core/customer/customer-store';
import { isValidPlate } from '../../core/domain/documents';
import {
  FUEL_TYPE_LABEL,
  TRANSMISSION_LABEL,
  VEHICLE_TYPE_LABEL,
} from '../../core/domain/enums';
import type { FuelType, TransmissionType, VehicleType } from '../../core/domain/enums';
import type { Vehicle } from '../../core/domain/models';
import { Callout } from '../../shared/ui/callout';
import { EntryBand } from '../../shared/ui/entry-band';

type Open = 'new' | { id: string } | { removing: string } | null;

/**
 * The customer's own vehicles.
 *
 * `GET /vehicles` scopes itself to the caller when the caller is a customer,
 * and `POST /vehicles` takes the owner from the token — a customer cannot file
 * a vehicle against anybody else even by trying, which is why no owner blank
 * appears on this form.
 *
 * The editing surface is the console's own ruled entry band: this world has no
 * modals, so a new vehicle unfolds as a blank line at the head of the register
 * the way one is written into a paper one. Removing deactivates — the vehicle
 * and its job history stay on the shop's record, and the confirmation says so
 * rather than implying a delete.
 */
@Component({
  selector: 'app-my-vehicles',
  imports: [Callout, EntryBand, NgTemplateOutlet],
  templateUrl: './my-vehicles.html',
  styleUrls: ['./garage.scss', './my-vehicles.scss'],
})
export class MyVehicles {
  protected readonly store = inject(CustomerStore);

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

  protected readonly typeLabel = VEHICLE_TYPE_LABEL;
  protected readonly fuelLabel = FUEL_TYPE_LABEL;
  protected readonly transmissionLabel = TRANSMISSION_LABEL;

  protected readonly open = signal<Open>(null);
  protected readonly busy = signal(false);
  protected readonly bandError = signal<string | null>(null);

  /* --- the blanks -------------------------------------------------------- */

  protected readonly plate = signal('');
  protected readonly make = signal('');
  protected readonly model = signal('');
  protected readonly colour = signal('');
  protected readonly modelYear = signal('');
  protected readonly vehicleType = signal<VehicleType>('CAR');
  protected readonly fuelType = signal<FuelType>('FLEX');
  protected readonly transmission = signal<TransmissionType>('MANUAL');

  protected readonly rows = computed(() => this.store.vehicles());

  protected readonly plateError = computed(() => {
    const value = this.plate().trim();
    if (value.length === 0) return 'A plate is required.';
    if (!isValidPlate(value)) return 'Not a Brazilian plate — ABC1D23 or ABC1234.';
    return null;
  });

  protected readonly yearError = computed(() => {
    const parsed = Number.parseInt(this.modelYear(), 10);
    if (!Number.isFinite(parsed)) return 'A model year is required.';
    if (parsed < 1900 || parsed > new Date().getFullYear() + 1) return 'That year is not plausible.';
    return null;
  });

  protected readonly canSave = computed(
    () =>
      this.plateError() === null &&
      this.yearError() === null &&
      this.make().trim().length > 0 &&
      this.model().trim().length > 0 &&
      this.colour().trim().length > 0,
  );

  protected isEditing(id: string): boolean {
    const open = this.open();
    return typeof open === 'object' && open !== null && 'id' in open && open.id === id;
  }

  protected isRemoving(id: string): boolean {
    const open = this.open();
    return typeof open === 'object' && open !== null && 'removing' in open && open.removing === id;
  }

  protected openNew(): void {
    this.reset();
    this.open.set('new');
  }

  protected openEdit(vehicle: Vehicle): void {
    this.bandError.set(null);
    this.plate.set(vehicle.licensePlate);
    this.make.set(vehicle.make);
    this.model.set(vehicle.model);
    this.colour.set(vehicle.color ?? '');
    this.modelYear.set(vehicle.modelYear === null ? '' : String(vehicle.modelYear));
    this.vehicleType.set(vehicle.vehicleType);
    this.fuelType.set(vehicle.fuelType);
    this.transmission.set(vehicle.transmissionType);
    this.open.set({ id: vehicle.id });
  }

  protected cancel(): void {
    this.open.set(null);
    this.bandError.set(null);
  }

  protected async save(): Promise<void> {
    if (!this.canSave() || this.busy()) return;
    const open = this.open();
    const command: VehicleCommand = {
      vehicleType: this.vehicleType(),
      licensePlate: this.plate().trim().toUpperCase().replace(/[^A-Z0-9]/g, ''),
      make: this.make().trim(),
      model: this.model().trim(),
      color: this.colour().trim(),
      modelYear: Number.parseInt(this.modelYear(), 10),
      fuelType: this.fuelType(),
      transmissionType: this.transmission(),
    };

    this.busy.set(true);
    this.bandError.set(null);
    try {
      if (open === 'new') await this.store.addVehicle(command);
      else if (open && 'id' in open) await this.store.updateVehicle(open.id, command);
      this.open.set(null);
      this.reset();
    } catch (error) {
      this.bandError.set(
        error instanceof ApiError ? error.message : 'The vehicle was not saved.',
      );
    } finally {
      this.busy.set(false);
    }
  }

  protected async remove(id: string): Promise<void> {
    this.busy.set(true);
    this.bandError.set(null);
    try {
      await this.store.removeVehicle(id);
      this.open.set(null);
    } catch (error) {
      this.bandError.set(
        error instanceof ApiError ? error.message : 'The vehicle was not removed.',
      );
    } finally {
      this.busy.set(false);
    }
  }

  private reset(): void {
    this.bandError.set(null);
    this.plate.set('');
    this.make.set('');
    this.model.set('');
    this.colour.set('');
    this.modelYear.set('');
    this.vehicleType.set('CAR');
    this.fuelType.set('FLEX');
    this.transmission.set('MANUAL');
  }
}
