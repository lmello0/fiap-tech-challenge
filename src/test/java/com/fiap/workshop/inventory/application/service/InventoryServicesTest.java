package com.fiap.workshop.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.inventory.api.events.LowStockDetected;
import com.fiap.workshop.inventory.api.events.PartCreated;
import com.fiap.workshop.inventory.api.events.PartDeactivated;
import com.fiap.workshop.inventory.api.events.PartUpdated;
import com.fiap.workshop.inventory.application.port.in.AdjustStockUseCase;
import com.fiap.workshop.inventory.application.port.in.CreatePartUseCase;
import com.fiap.workshop.inventory.application.port.in.RegisterStockEntryUseCase;
import com.fiap.workshop.inventory.application.port.in.UpdatePartUseCase;
import com.fiap.workshop.inventory.application.port.out.PartRepository.PartFilter;
import com.fiap.workshop.inventory.domain.Part;
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

class InventoryServicesTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
	private final InMemoryRepositories.Parts parts = new InMemoryRepositories.Parts();
	private final InMemoryRepositories.Movements movements = new InMemoryRepositories.Movements();
	private final RecordingEventPublisher events = new RecordingEventPublisher();

	private final CreatePartService create = new CreatePartService(parts, events, clock);
	private final UpdatePartService update = new UpdatePartService(parts, events, clock);
	private final DeactivatePartService deactivate = new DeactivatePartService(parts, events, clock);
	private final RegisterStockEntryService stockEntry = new RegisterStockEntryService(parts, movements, clock);
	private final AdjustStockService adjust = new AdjustStockService(parts, movements, events, clock);

	private Part filter(int minimum) {
		return create.execute(new CreatePartUseCase.Command("flt-001", "Oil filter", null, new BigDecimal("35"), minimum));
	}

	@Test
	void createsPartAndPublishesEvent() {
		Part part = filter(5);

		assertThat(parts.store).containsKey(part.id());
		assertThat(events.of(PartCreated.class)).singleElement().satisfies(e -> {
			assertThat(e.sku()).isEqualTo("FLT-001");
			assertThat(e.unitPrice()).isEqualByComparingTo("35");
		});
	}

	@Test
	void rejectsDuplicateSkuIgnoringCase() {
		filter(5);

		assertThatThrownBy(() -> create.execute(new CreatePartUseCase.Command("FLT-001", "Other", null, BigDecimal.ONE, 0)))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void stockEntryIncreasesStockAndStoresTheMovement() {
		Part part = filter(5);

		var movement = stockEntry.execute(new RegisterStockEntryUseCase.Command(part.id(), 12, "invoice", "user-1"));

		assertThat(parts.store.get(part.id()).onHand()).isEqualTo(12);
		assertThat(movements.store).containsExactly(movement);
		assertThat(movement.performedBy()).isEqualTo("user-1");
		assertThat(events.of(LowStockDetected.class)).isEmpty();
	}

	@Test
	void stockEntryOfUnknownPartFails() {
		assertThatThrownBy(() -> stockEntry.execute(new RegisterStockEntryUseCase.Command(UUID.randomUUID(), 1, null, "u")))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void adjustmentBelowTheMinimumRaisesLowStockOnce() {
		Part part = filter(5);
		stockEntry.execute(new RegisterStockEntryUseCase.Command(part.id(), 10, null, "u"));

		adjust.execute(new AdjustStockUseCase.Command(part.id(), -6, "damaged", "u"));
		adjust.execute(new AdjustStockUseCase.Command(part.id(), -1, "damaged", "u"));

		assertThat(events.of(LowStockDetected.class)).singleElement().satisfies(e -> {
			assertThat(e.onHand()).isEqualTo(4);
			assertThat(e.minimumQuantity()).isEqualTo(5);
		});
		assertThat(movements.store).hasSize(3);
	}

	@Test
	void rejectedAdjustmentChangesNothing() {
		Part part = filter(0);
		stockEntry.execute(new RegisterStockEntryUseCase.Command(part.id(), 2, null, "u"));

		assertThatThrownBy(() -> adjust.execute(new AdjustStockUseCase.Command(part.id(), -3, "oops", "u")))
				.isInstanceOf(DomainException.class);
		assertThat(parts.store.get(part.id()).onHand()).isEqualTo(2);
		assertThat(movements.store).hasSize(1);
	}

	@Test
	void updateChangesFieldsAndPublishesEvent() {
		Part part = filter(5);

		update.execute(new UpdatePartUseCase.Command(part.id(), "Oil filter XL", null, new BigDecimal("40"), 5));

		assertThat(events.of(PartUpdated.class)).singleElement().extracting(PartUpdated::name).isEqualTo("Oil filter XL");
		assertThatThrownBy(() -> update.execute(new UpdatePartUseCase.Command(UUID.randomUUID(), "A", null, BigDecimal.ONE, 0)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void raisingTheMinimumAboveStockRaisesLowStock() {
		Part part = filter(2);
		stockEntry.execute(new RegisterStockEntryUseCase.Command(part.id(), 5, null, "u"));

		update.execute(new UpdatePartUseCase.Command(part.id(), "Oil filter", null, new BigDecimal("35"), 10));

		assertThat(events.of(LowStockDetected.class)).hasSize(1);
		update.execute(new UpdatePartUseCase.Command(part.id(), "Oil filter", null, new BigDecimal("35"), 12));
		assertThat(events.of(LowStockDetected.class)).hasSize(1);
	}

	@Test
	void deactivatesOnce() {
		Part part = filter(0);

		deactivate.execute(part.id());

		assertThat(events.of(PartDeactivated.class)).hasSize(1);
		assertThatThrownBy(() -> deactivate.execute(part.id())).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> deactivate.execute(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
	}

	@Test
	void listsGetsAndQueries() {
		Part low = filter(5);
		Part healthy = create.execute(new CreatePartUseCase.Command("BRK-9", "Brake pad", null, BigDecimal.TEN, 1));
		stockEntry.execute(new RegisterStockEntryUseCase.Command(healthy.id(), 10, null, "u"));
		var list = new ListPartsService(parts);

		assertThat(list.execute(new PartFilter(null, false, false), new PageQuery(0, 10)).totalElements()).isEqualTo(2);
		assertThat(list.execute(new PartFilter(null, true, false), new PageQuery(0, 10)).content()).containsExactly(low);
		assertThat(new GetPartService(parts).execute(low.id())).isSameAs(low);
		assertThatThrownBy(() -> new GetPartService(parts).execute(UUID.randomUUID())).isInstanceOf(NotFoundException.class);

		var queries = new InventoryQueriesService(parts);
		assertThat(queries.findPartById(healthy.id())).get().extracting("sku").isEqualTo("BRK-9");
		assertThat(queries.findPartById(UUID.randomUUID())).isEmpty();
	}

	@Test
	void listsMovementsOfAnExistingPartOnly() {
		Part part = filter(0);
		stockEntry.execute(new RegisterStockEntryUseCase.Command(part.id(), 3, null, "u"));
		var service = new ListStockMovementsService(parts, movements);

		assertThat(service.execute(part.id(), new PageQuery(0, 10)).content()).hasSize(1);
		assertThatThrownBy(() -> service.execute(UUID.randomUUID(), new PageQuery(0, 10))).isInstanceOf(NotFoundException.class);
	}
}
