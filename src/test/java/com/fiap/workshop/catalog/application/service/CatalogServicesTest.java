package com.fiap.workshop.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.catalog.api.events.ServiceCreated;
import com.fiap.workshop.catalog.api.events.ServiceDeactivated;
import com.fiap.workshop.catalog.api.events.ServiceUpdated;
import com.fiap.workshop.catalog.application.port.in.CreateServiceUseCase;
import com.fiap.workshop.catalog.application.port.in.UpdateServiceUseCase;
import com.fiap.workshop.catalog.application.port.out.RepairServiceRepository.ServiceFilter;
import com.fiap.workshop.catalog.domain.RepairService;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.RecordingEventPublisher;
import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogServicesTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
	private final InMemoryRepairServiceRepository services = new InMemoryRepairServiceRepository();
	private final RecordingEventPublisher events = new RecordingEventPublisher();

	private final CreateServiceService create = new CreateServiceService(services, events, clock);
	private final UpdateServiceService update = new UpdateServiceService(services, events, clock);
	private final DeactivateServiceService deactivate = new DeactivateServiceService(services, events, clock);
	private final GetServiceService get = new GetServiceService(services);
	private final ListServicesService list = new ListServicesService(services);

	private RepairService oilChange() {
		return create.execute(new CreateServiceUseCase.Command("Oil change", "Synthetic", new BigDecimal("150"), 45));
	}

	@Test
	void createsServiceAndPublishesEvent() {
		RepairService service = oilChange();

		assertThat(services.store).containsKey(service.id());
		assertThat(events.of(ServiceCreated.class)).singleElement().satisfies(e -> {
			assertThat(e.serviceId()).isEqualTo(service.id());
			assertThat(e.price()).isEqualByComparingTo("150.00");
			assertThat(e.estimatedMinutes()).isEqualTo(45);
		});
	}

	@Test
	void rejectsDuplicateNameIgnoringCase() {
		oilChange();

		assertThatThrownBy(() -> create.execute(new CreateServiceUseCase.Command("OIL CHANGE", null, BigDecimal.ONE, 10)))
				.isInstanceOf(ConflictException.class);
		assertThat(events.of(ServiceCreated.class)).hasSize(1);
	}

	@Test
	void rejectsInvalidPrice() {
		assertThatThrownBy(() -> create.execute(new CreateServiceUseCase.Command("A", null, new BigDecimal("-1"), 10)))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> create.execute(new CreateServiceUseCase.Command("A", null, null, 10)))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void updatesServiceAndPublishesEvent() {
		RepairService service = oilChange();

		update.execute(new UpdateServiceUseCase.Command(service.id(), "Oil change plus", null, new BigDecimal("180"), 60));

		assertThat(services.store.get(service.id()).price().amount()).isEqualByComparingTo("180");
		assertThat(events.of(ServiceUpdated.class)).singleElement().extracting(ServiceUpdated::name).isEqualTo("Oil change plus");
	}

	@Test
	void updateAllowsKeepingOwnNameButNotTakingAnotherOne() {
		RepairService oil = oilChange();
		RepairService alignment = create.execute(new CreateServiceUseCase.Command("Alignment", null, BigDecimal.TEN, 30));

		update.execute(new UpdateServiceUseCase.Command(oil.id(), "Oil change", null, BigDecimal.TEN, 30));
		assertThatThrownBy(() -> update.execute(new UpdateServiceUseCase.Command(alignment.id(), "oil change", null, BigDecimal.TEN, 30)))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void updateOfUnknownServiceFails() {
		assertThatThrownBy(() -> update.execute(new UpdateServiceUseCase.Command(UUID.randomUUID(), "A", null, BigDecimal.ONE, 1)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void deactivatesOnceAndPublishesEvent() {
		RepairService service = oilChange();

		deactivate.execute(service.id());

		assertThat(events.of(ServiceDeactivated.class)).hasSize(1);
		assertThatThrownBy(() -> deactivate.execute(service.id())).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> deactivate.execute(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
	}

	@Test
	void getAndListHonorFilters() {
		RepairService oil = oilChange();
		create.execute(new CreateServiceUseCase.Command("Alignment", null, BigDecimal.TEN, 30));
		deactivate.execute(oil.id());

		assertThat(get.execute(oil.id())).isSameAs(oil);
		assertThatThrownBy(() -> get.execute(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
		assertThat(list.execute(new ServiceFilter(null, false), new PageQuery(0, 10)).content()).hasSize(1);
		assertThat(list.execute(new ServiceFilter(null, true), new PageQuery(0, 10)).totalElements()).isEqualTo(2);
		assertThat(list.execute(new ServiceFilter("align", false), new PageQuery(0, 10)).content()).hasSize(1);
	}

	@Test
	void queriesExposeAPublicView() {
		RepairService service = oilChange();
		var queries = new CatalogQueriesService(services);

		assertThat(queries.findServiceById(service.id())).get().satisfies(view -> {
			assertThat(view.name()).isEqualTo("Oil change");
			assertThat(view.price()).isEqualByComparingTo("150");
			assertThat(view.active()).isTrue();
		});
		assertThat(queries.findServiceById(UUID.randomUUID())).isEmpty();
	}
}
