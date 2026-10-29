package com.fiap.workshop.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.LicensePlate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VehicleTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
	private final UUID customerId = UUID.randomUUID();

	private Vehicle vehicle() {
		return Vehicle.register(customerId, LicensePlate.of("abc-1d23"), " Fiat ", "Argo", 2022, clock);
	}

	@Test
	void registersActiveVehicleOwnedByCustomer() {
		Vehicle vehicle = vehicle();

		assertThat(vehicle.customerId()).isEqualTo(customerId);
		assertThat(vehicle.plate().value()).isEqualTo("ABC1D23");
		assertThat(vehicle.brand()).isEqualTo("Fiat");
		assertThat(vehicle.active()).isTrue();
		assertThat(vehicle.createdAt()).isEqualTo(clock.instant());
	}

	@Test
	void acceptsNextModelYearButNotTheOneAfter() {
		assertThat(Vehicle.register(customerId, LicensePlate.of("ABC1234"), "A", "B", 2027, clock).year()).isEqualTo(2027);
		assertThatThrownBy(() -> Vehicle.register(customerId, LicensePlate.of("ABC1234"), "A", "B", 2028, clock))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Vehicle.register(customerId, LicensePlate.of("ABC1234"), "A", "B", 1899, clock))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void rejectsBlankBrandOrModel() {
		assertThatThrownBy(() -> Vehicle.register(customerId, LicensePlate.of("ABC1234"), " ", "B", 2020, clock))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Vehicle.register(customerId, LicensePlate.of("ABC1234"), "A", null, 2020, clock))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void updatesDescriptionButKeepsPlateAndOwner() {
		Vehicle vehicle = vehicle();

		vehicle.update("VW", "Polo", 2023, clock);

		assertThat(vehicle.brand()).isEqualTo("VW");
		assertThat(vehicle.model()).isEqualTo("Polo");
		assertThat(vehicle.year()).isEqualTo(2023);
		assertThat(vehicle.plate().value()).isEqualTo("ABC1D23");
		assertThat(vehicle.customerId()).isEqualTo(customerId);
	}

	@Test
	void deactivatedVehicleCannotBeChanged() {
		Vehicle vehicle = vehicle();
		vehicle.deactivate(clock.instant());

		assertThat(vehicle.active()).isFalse();
		assertThatThrownBy(() -> vehicle.update("A", "B", 2020, clock)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> vehicle.deactivate(clock.instant())).isInstanceOf(DomainException.class);
	}
}
