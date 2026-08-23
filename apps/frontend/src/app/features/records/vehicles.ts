import { Component, computed, inject, signal } from '@angular/core';
import { ShopStore } from '../../core/data/shop-store';
import { FUEL_TYPE_LABEL, TRANSMISSION_LABEL, VEHICLE_TYPE_LABEL } from '../../core/domain/enums';

@Component({
  selector: 'app-vehicles',
  template: `
    <div class="rec">
      <header class="rec__head">
        <div>
          <h1 class="rec__title">Vehicles</h1>
          <p class="rec__sub">{{ rows().length }} on record</p>
        </div>
        <label class="field">
          <span class="sr-only">Search vehicles</span>
          <input
            class="input rec__search"
            type="search"
            placeholder="Plate, make, model or owner"
            [value]="q()"
            (input)="onQuery($event)"
          />
        </label>
      </header>

      <section class="plate">
        <div class="plate__head">
          <p class="plate__title">8.1 — Vehicles on record</p>
          <p class="plate__meta label">Attendant or manager</p>
        </div>
        <div class="mt-wrap">
          <table class="mt">
            <caption class="sr-only">Vehicles with plate, specification and owner.</caption>
            <thead>
              <tr>
                <th scope="col" class="shrink">Plate</th>
                <th scope="col">Vehicle</th>
                <th scope="col" class="shrink">Type</th>
                <th scope="col" class="shrink">Years</th>
                <th scope="col">Colour</th>
                <th scope="col">Fuel</th>
                <th scope="col">Owner</th>
              </tr>
            </thead>
            <tbody>
              @for (v of rows(); track v.id) {
                <tr [class.is-inactive]="!v.active">
                  <td class="shrink"><span class="token rec__plate">{{ v.licensePlate }}</span></td>
                  <td>
                    {{ v.make }} {{ v.model }}
                    <span class="rec__spec">{{ transmission(v.transmissionType) }}</span>
                  </td>
                  <td class="shrink rec__muted">{{ typeLabel(v.vehicleType) }}</td>
                  <td class="shrink num rec__muted">{{ v.manufactureYear }}/{{ v.modelYear }}</td>
                  <td class="rec__muted">{{ v.color ?? '—' }}</td>
                  <td class="rec__muted">{{ fuelLabel(v.fuelType) }}</td>
                  <td>{{ v.customerName }}</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="7">
                    <div class="empty">
                      <p class="empty__title">No vehicle matches that search</p>
                      <p>Plates are stored without a separator — try <span class="token">RJP7A41</span>.</p>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `,
  styleUrl: './records.scss',
})
export class Vehicles {
  private readonly store = inject(ShopStore);
  protected readonly q = signal('');

  protected readonly rows = computed(() => {
    const q = this.q().trim().toLowerCase().replace(/[^a-z0-9]/g, '');
    return this.store.vehicles().filter((v) =>
      q
        ? [v.licensePlate, v.make, v.model, v.customerName ?? '']
            .join(' ')
            .toLowerCase()
            .replace(/[^a-z0-9 ]/g, '')
            .includes(q)
        : true,
    );
  });

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
}
