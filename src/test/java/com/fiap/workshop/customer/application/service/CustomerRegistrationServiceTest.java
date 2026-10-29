package com.fiap.workshop.customer.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.customer.api.CustomerRegistration.CustomerData;
import com.fiap.workshop.customer.api.CustomerRegistration.VehicleData;
import com.fiap.workshop.customer.api.CustomerView;
import com.fiap.workshop.customer.api.events.CustomerRegistered;
import com.fiap.workshop.customer.api.events.VehicleRegistered;
import com.fiap.workshop.shared.application.RecordingEventPublisher;
import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.DomainException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerRegistrationServiceTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
	private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
	private final InMemoryVehicleRepository vehicles = new InMemoryVehicleRepository();
	private final RecordingEventPublisher events = new RecordingEventPublisher();
	private final CustomerRegistrationService registration = new CustomerRegistrationService(customers, vehicles,
			new RegisterCustomerService(customers, events, clock), new RegisterVehicleService(customers, vehicles, events, clock));

	private CustomerData ana() {
		return new CustomerData("529.982.247-25", "Ana Souza", "ana@example.com", "11999990000");
	}

	private VehicleData argo() {
		return new VehicleData("abc-1d23", "Fiat", "Argo", 2022);
	}

	@Test
	void registersANewCustomerAndPublishesTheEvent() {
		CustomerView view = registration.ensureCustomer(ana());

		assertThat(view.document()).isEqualTo("52998224725");
		assertThat(view.active()).isTrue();
		assertThat(customers.store).containsKey(view.id());
		assertThat(events.of(CustomerRegistered.class)).singleElement().extracting(CustomerRegistered::customerId).isEqualTo(view.id());
	}

	@Test
	void reusesAnExistingCustomerWithoutOverwritingItsData() {
		CustomerView first = registration.ensureCustomer(ana());

		CustomerView again = registration.ensureCustomer(new CustomerData("52998224725", "Someone Else", "else@example.com", null));

		assertThat(again.id()).isEqualTo(first.id());
		assertThat(again.name()).isEqualTo("Ana Souza");
		assertThat(again.email()).isEqualTo("ana@example.com");
		assertThat(events.of(CustomerRegistered.class)).hasSize(1);
	}

	@Test
	void refusesADeactivatedCustomer() {
		CustomerView view = registration.ensureCustomer(ana());
		customers.store.get(view.id()).deactivate(clock.instant());

		assertThatThrownBy(() -> registration.ensureCustomer(ana())).isInstanceOf(DomainException.class).hasMessageContaining("deactivated");
	}

	@Test
	void refusesAnInvalidDocumentOrAnEmailOfAnotherCustomer() {
		assertThatThrownBy(() -> registration.ensureCustomer(new CustomerData("123", "Ana", "a@b.co", null))).isInstanceOf(DomainException.class);

		registration.ensureCustomer(ana());
		assertThatThrownBy(() -> registration.ensureCustomer(new CustomerData("111.444.777-35", "Bob", "ana@example.com", null)))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void registersANewVehicleForTheCustomer() {
		UUID customerId = registration.ensureCustomer(ana()).id();

		var vehicle = registration.ensureVehicle(customerId, argo());

		assertThat(vehicle.plate()).isEqualTo("ABC1D23");
		assertThat(vehicle.customerId()).isEqualTo(customerId);
		assertThat(events.of(VehicleRegistered.class)).singleElement().extracting(VehicleRegistered::vehicleId).isEqualTo(vehicle.id());
	}

	@Test
	void reusesAnExistingVehicleOfTheSameCustomer() {
		UUID customerId = registration.ensureCustomer(ana()).id();
		var first = registration.ensureVehicle(customerId, argo());

		var again = registration.ensureVehicle(customerId, new VehicleData("ABC1D23", "Ford", "Ka", 2010));

		assertThat(again.id()).isEqualTo(first.id());
		assertThat(again.model()).isEqualTo("Argo");
		assertThat(events.of(VehicleRegistered.class)).hasSize(1);
	}

	@Test
	void refusesAVehicleOfAnotherCustomerOrADeactivatedOne() {
		UUID ana = registration.ensureCustomer(ana()).id();
		UUID bob = registration.ensureCustomer(new CustomerData("111.444.777-35", "Bob", "bob@example.com", null)).id();
		var vehicle = registration.ensureVehicle(ana, argo());

		assertThatThrownBy(() -> registration.ensureVehicle(bob, argo())).isInstanceOf(DomainException.class).hasMessageContaining("does not belong");

		vehicles.store.get(vehicle.id()).deactivate(clock.instant());
		assertThatThrownBy(() -> registration.ensureVehicle(ana, argo())).isInstanceOf(DomainException.class).hasMessageContaining("deactivated");
	}

	@Test
	void refusesAnInvalidPlate() {
		UUID customerId = registration.ensureCustomer(ana()).id();

		assertThatThrownBy(() -> registration.ensureVehicle(customerId, new VehicleData("12", "Fiat", "Argo", 2022)))
				.isInstanceOf(DomainException.class);
	}
}
