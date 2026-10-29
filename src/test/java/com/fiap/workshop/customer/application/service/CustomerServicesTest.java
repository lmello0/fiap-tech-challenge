package com.fiap.workshop.customer.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.customer.api.events.CustomerDeactivated;
import com.fiap.workshop.customer.api.events.CustomerRegistered;
import com.fiap.workshop.customer.api.events.CustomerUpdated;
import com.fiap.workshop.customer.api.events.VehicleDeactivated;
import com.fiap.workshop.customer.application.port.in.RegisterCustomerUseCase;
import com.fiap.workshop.customer.application.port.in.RegisterVehicleUseCase;
import com.fiap.workshop.customer.application.port.in.UpdateCustomerUseCase;
import com.fiap.workshop.customer.application.port.out.CustomerRepository.CustomerFilter;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.RecordingEventPublisher;
import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerServicesTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
	private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
	private final InMemoryVehicleRepository vehicles = new InMemoryVehicleRepository();
	private final RecordingEventPublisher events = new RecordingEventPublisher();

	private final RegisterCustomerService register = new RegisterCustomerService(customers, events, clock);
	private final UpdateCustomerService update = new UpdateCustomerService(customers, events, clock);
	private final DeactivateCustomerService deactivate = new DeactivateCustomerService(customers, vehicles, events, clock);
	private final RegisterVehicleService registerVehicle = new RegisterVehicleService(customers, vehicles, events, clock);

	private Customer registerAna() {
		return register.execute(new RegisterCustomerUseCase.Command("Ana Souza", "529.982.247-25", "ana@example.com", "11999990000"));
	}

	@Test
	void registersCustomerAndPublishesEvent() {
		Customer customer = registerAna();

		assertThat(customers.store).containsKey(customer.id());
		CustomerRegistered event = events.of(CustomerRegistered.class).getFirst();
		assertThat(event.customerId()).isEqualTo(customer.id());
		assertThat(event.document()).isEqualTo("52998224725");
		assertThat(event.email()).isEqualTo("ana@example.com");
		assertThat(event.occurredAt()).isEqualTo(clock.instant());
	}

	@Test
	void rejectsDuplicateDocumentAndEmail() {
		registerAna();

		assertThatThrownBy(() -> register.execute(new RegisterCustomerUseCase.Command("Other", "52998224725", "other@example.com", null)))
				.isInstanceOf(ConflictException.class).hasMessageContaining("document");
		assertThatThrownBy(() -> register.execute(new RegisterCustomerUseCase.Command("Other", "111.444.777-35", "ANA@example.com", null)))
				.isInstanceOf(ConflictException.class).hasMessageContaining("e-mail");
		assertThat(events.of(CustomerRegistered.class)).hasSize(1);
	}

	@Test
	void rejectsInvalidDocument() {
		assertThatThrownBy(() -> register.execute(new RegisterCustomerUseCase.Command("Ana", "123", "ana@example.com", null)))
				.isInstanceOf(DomainException.class);
		assertThat(customers.store).isEmpty();
	}

	@Test
	void updatesCustomerAndPublishesEvent() {
		Customer customer = registerAna();

		update.execute(new UpdateCustomerUseCase.Command(customer.id(), "Ana Lima", "lima@example.com", null));

		assertThat(customers.store.get(customer.id()).name()).isEqualTo("Ana Lima");
		assertThat(events.of(CustomerUpdated.class)).singleElement().extracting(CustomerUpdated::email).isEqualTo("lima@example.com");
	}

	@Test
	void updateRejectsEmailOfAnotherCustomerButAllowsOwnEmail() {
		Customer ana = registerAna();
		register.execute(new RegisterCustomerUseCase.Command("Bob", "111.444.777-35", "bob@example.com", null));

		assertThatThrownBy(() -> update.execute(new UpdateCustomerUseCase.Command(ana.id(), "Ana", "bob@example.com", null)))
				.isInstanceOf(ConflictException.class);
		update.execute(new UpdateCustomerUseCase.Command(ana.id(), "Ana Souza", "ana@example.com", "123"));
		assertThat(customers.store.get(ana.id()).phone()).isEqualTo("123");
	}

	@Test
	void updateOfUnknownCustomerFails() {
		assertThatThrownBy(() -> update.execute(new UpdateCustomerUseCase.Command(UUID.randomUUID(), "A", "a@b.co", null)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void deactivationCascadesToVehiclesAndPublishesEvents() {
		Customer customer = registerAna();
		registerVehicle.execute(new RegisterVehicleUseCase.Command(customer.id(), "ABC1D23", "Fiat", "Argo", 2022));
		registerVehicle.execute(new RegisterVehicleUseCase.Command(customer.id(), "XYZ9876", "VW", "Polo", 2020));

		deactivate.execute(customer.id());

		assertThat(customers.store.get(customer.id()).active()).isFalse();
		assertThat(vehicles.store.values()).allMatch(v -> !v.active());
		assertThat(events.of(VehicleDeactivated.class)).hasSize(2);
		assertThat(events.of(CustomerDeactivated.class)).singleElement().extracting(CustomerDeactivated::customerId).isEqualTo(customer.id());
	}

	@Test
	void deactivatedCustomerCannotBeDeactivatedAgain() {
		Customer customer = registerAna();
		deactivate.execute(customer.id());

		assertThatThrownBy(() -> deactivate.execute(customer.id())).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> deactivate.execute(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
	}

	@Test
	void listingHidesInactiveByDefault() {
		Customer ana = registerAna();
		register.execute(new RegisterCustomerUseCase.Command("Bob", "111.444.777-35", "bob@example.com", null));
		deactivate.execute(ana.id());
		var list = new ListCustomersService(customers);

		assertThat(list.execute(new CustomerFilter(null, null, false), new PageQuery(0, 10)).content()).hasSize(1);
		assertThat(list.execute(new CustomerFilter(null, null, true), new PageQuery(0, 10)).totalElements()).isEqualTo(2);
	}

	@Test
	void getReturnsCustomerOrFails() {
		Customer customer = registerAna();
		var get = new GetCustomerService(customers);

		assertThat(get.execute(customer.id())).isSameAs(customer);
		assertThatThrownBy(() -> get.execute(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
	}
}
