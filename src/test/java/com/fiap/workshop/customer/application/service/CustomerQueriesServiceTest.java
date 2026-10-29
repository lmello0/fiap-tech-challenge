package com.fiap.workshop.customer.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fiap.workshop.customer.application.port.in.RegisterCustomerUseCase;
import com.fiap.workshop.customer.application.port.in.RegisterVehicleUseCase;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.shared.application.RecordingEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerQueriesServiceTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
	private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
	private final InMemoryVehicleRepository vehicles = new InMemoryVehicleRepository();
	private final CustomerQueriesService queries = new CustomerQueriesService(customers, vehicles);

	private Customer ana;

	@BeforeEach
	void setUp() {
		RecordingEventPublisher events = new RecordingEventPublisher();
		ana = new RegisterCustomerService(customers, events, clock)
				.execute(new RegisterCustomerUseCase.Command("Ana", "529.982.247-25", "ana@example.com", null));
		new RegisterVehicleService(customers, vehicles, events, clock)
				.execute(new RegisterVehicleUseCase.Command(ana.id(), "ABC1D23", "Fiat", "Argo", 2022));
	}

	@Test
	void findsCustomerByIdAndFormattedDocument() {
		assertThat(queries.findCustomerById(ana.id())).get().extracting("email").isEqualTo("ana@example.com");
		assertThat(queries.findCustomerByDocument("529.982.247-25")).isPresent();
		assertThat(queries.findCustomerById(UUID.randomUUID())).isEmpty();
	}

	@Test
	void findsVehicleByIdAndPlateInAnyFormat() {
		var byPlate = queries.findVehicleByPlate("abc-1d23");

		assertThat(byPlate).isPresent();
		assertThat(queries.findVehicleById(byPlate.get().id())).isPresent();
		assertThat(queries.findVehicleById(UUID.randomUUID())).isEmpty();
	}

	@Test
	void invalidDocumentOrPlateYieldsEmptyInsteadOfFailing() {
		assertThat(queries.findCustomerByDocument("123")).isEmpty();
		assertThat(queries.findVehicleByPlate("nope")).isEmpty();
	}
}
