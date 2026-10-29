package com.fiap.workshop.customer.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.customer.api.events.VehicleDeactivated;
import com.fiap.workshop.customer.api.events.VehicleRegistered;
import com.fiap.workshop.customer.api.events.VehicleUpdated;
import com.fiap.workshop.customer.application.port.in.RegisterCustomerUseCase;
import com.fiap.workshop.customer.application.port.in.RegisterVehicleUseCase;
import com.fiap.workshop.customer.application.port.in.UpdateVehicleUseCase;
import com.fiap.workshop.customer.application.port.out.VehicleRepository.VehicleFilter;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.RecordingEventPublisher;
import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VehicleServicesTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
	private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
	private final InMemoryVehicleRepository vehicles = new InMemoryVehicleRepository();
	private final RecordingEventPublisher events = new RecordingEventPublisher();

	private final RegisterVehicleService register = new RegisterVehicleService(customers, vehicles, events, clock);
	private final UpdateVehicleService update = new UpdateVehicleService(vehicles, events, clock);
	private final DeactivateVehicleService deactivate = new DeactivateVehicleService(vehicles, events, clock);

	private Customer owner;

	@BeforeEach
	void setUp() {
		owner = new RegisterCustomerService(customers, events, clock)
				.execute(new RegisterCustomerUseCase.Command("Ana", "529.982.247-25", "ana@example.com", null));
		events.events().clear();
	}

	private Vehicle registerArgo() {
		return register.execute(new RegisterVehicleUseCase.Command(owner.id(), "abc-1d23", "Fiat", "Argo", 2022));
	}

	@Test
	void registersVehicleForActiveCustomerAndPublishesEvent() {
		Vehicle vehicle = registerArgo();

		assertThat(vehicle.plate().value()).isEqualTo("ABC1D23");
		assertThat(events.of(VehicleRegistered.class)).singleElement().satisfies(e -> {
			assertThat(e.vehicleId()).isEqualTo(vehicle.id());
			assertThat(e.customerId()).isEqualTo(owner.id());
			assertThat(e.plate()).isEqualTo("ABC1D23");
		});
	}

	@Test
	void rejectsDuplicatePlate() {
		registerArgo();

		assertThatThrownBy(() -> register.execute(new RegisterVehicleUseCase.Command(owner.id(), "ABC1D23", "VW", "Polo", 2020)))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void rejectsUnknownOrDeactivatedOwner() {
		assertThatThrownBy(() -> register.execute(new RegisterVehicleUseCase.Command(UUID.randomUUID(), "ABC1234", "A", "B", 2020)))
				.isInstanceOf(NotFoundException.class);

		owner.deactivate(clock.instant());
		assertThatThrownBy(() -> register.execute(new RegisterVehicleUseCase.Command(owner.id(), "ABC1234", "A", "B", 2020)))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void rejectsInvalidPlate() {
		assertThatThrownBy(() -> register.execute(new RegisterVehicleUseCase.Command(owner.id(), "12", "A", "B", 2020)))
				.isInstanceOf(DomainException.class);
		assertThat(vehicles.store).isEmpty();
	}

	@Test
	void updatesVehicleAndPublishesEvent() {
		Vehicle vehicle = registerArgo();

		update.execute(new UpdateVehicleUseCase.Command(vehicle.id(), "VW", "Polo", 2023));

		assertThat(vehicles.store.get(vehicle.id()).model()).isEqualTo("Polo");
		assertThat(events.of(VehicleUpdated.class)).singleElement().extracting(VehicleUpdated::model).isEqualTo("Polo");
		assertThatThrownBy(() -> update.execute(new UpdateVehicleUseCase.Command(UUID.randomUUID(), "A", "B", 2020)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void deactivatesVehicleOnce() {
		Vehicle vehicle = registerArgo();

		deactivate.execute(vehicle.id());

		assertThat(events.of(VehicleDeactivated.class)).hasSize(1);
		assertThatThrownBy(() -> deactivate.execute(vehicle.id())).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> deactivate.execute(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
	}

	@Test
	void listsByOwnerAndHidesInactive() {
		Vehicle first = registerArgo();
		register.execute(new RegisterVehicleUseCase.Command(owner.id(), "XYZ9876", "VW", "Polo", 2020));
		deactivate.execute(first.id());
		var list = new ListVehiclesService(vehicles);

		assertThat(list.execute(new VehicleFilter(owner.id(), false), new PageQuery(0, 10)).content()).hasSize(1);
		assertThat(list.execute(new VehicleFilter(owner.id(), true), new PageQuery(0, 10)).totalElements()).isEqualTo(2);
		assertThat(list.execute(new VehicleFilter(UUID.randomUUID(), true), new PageQuery(0, 10)).content()).isEmpty();
	}

	@Test
	void getReturnsVehicleOrFails() {
		Vehicle vehicle = registerArgo();
		var get = new GetVehicleService(vehicles);

		assertThat(get.execute(vehicle.id())).isSameAs(vehicle);
		assertThatThrownBy(() -> get.execute(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
	}
}
