package com.fiap.workshop.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fiap.workshop.inventory.api.events.LowStockDetected;
import com.fiap.workshop.inventory.api.events.PartsReserved;
import com.fiap.workshop.inventory.api.events.PartsShortage;
import com.fiap.workshop.inventory.api.events.ReservationConsumed;
import com.fiap.workshop.inventory.api.events.ReservationReleased;
import com.fiap.workshop.inventory.application.port.in.AdjustStockUseCase;
import com.fiap.workshop.inventory.application.port.in.PartQuantity;
import com.fiap.workshop.inventory.application.port.in.RegisterStockEntryUseCase;
import com.fiap.workshop.inventory.application.port.in.ReserveStockUseCase;
import com.fiap.workshop.inventory.domain.MovementType;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.inventory.domain.ReservationStatus;
import com.fiap.workshop.inventory.domain.StockReservation;
import com.fiap.workshop.shared.application.RecordingEventPublisher;
import com.fiap.workshop.shared.domain.Money;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class StockReservationsTest {

	private final AtomicLong tick = new AtomicLong();
	private final Clock clock = new Clock() {
		@Override
		public java.time.ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return Instant.parse("2026-06-01T12:00:00Z").plusSeconds(tick.incrementAndGet());
		}
	};
	private final InMemoryRepositories.Parts parts = new InMemoryRepositories.Parts();
	private final InMemoryRepositories.Movements movements = new InMemoryRepositories.Movements();
	private final InMemoryRepositories.Reservations store = new InMemoryRepositories.Reservations();
	private final RecordingEventPublisher events = new RecordingEventPublisher();
	private final StockReservations reservations = new StockReservations(parts, store, movements, events, clock);
	private final ReserveStockService reserve = new ReserveStockService(reservations);
	private final ConsumeStockService consume = new ConsumeStockService(reservations);
	private final ReleaseStockService release = new ReleaseStockService(reservations);
	private final RegisterStockEntryService entry = new RegisterStockEntryService(parts, movements, reservations, clock);
	private final AdjustStockService adjust = new AdjustStockService(parts, movements, events, reservations, clock);

	private Part part(String sku, int onHand, int minimum) {
		Part part = Part.create(sku, sku, null, Money.of("10"), minimum, clock.instant());
		if (onHand > 0) {
			part.receive(onHand, null, "u", clock.instant());
		}
		return parts.save(part);
	}

	private UUID request(UUID workOrderId, Part part, int quantity) {
		UUID budgetId = UUID.randomUUID();
		reserve.execute(new ReserveStockUseCase.Command(workOrderId, "OS-" + workOrderId.toString().substring(0, 4), budgetId,
				List.of(new PartQuantity(part.id(), quantity))));
		return budgetId;
	}

	private StockReservation reservationOf(UUID budgetId) {
		return store.findByBudgetId(budgetId).orElseThrow();
	}

	@Test
	void reservesWhenEverythingIsAvailable() {
		Part filter = part("FLT", 10, 0);
		UUID workOrder = UUID.randomUUID();

		UUID budget = request(workOrder, filter, 4);

		assertThat(reservationOf(budget).status()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(parts.store.get(filter.id()).reserved()).isEqualTo(4);
		assertThat(parts.store.get(filter.id()).onHand()).isEqualTo(10);
		assertThat(events.of(PartsReserved.class)).singleElement().satisfies(e -> {
			assertThat(e.workOrderId()).isEqualTo(workOrder);
			assertThat(e.budgetId()).isEqualTo(budget);
		});
		assertThat(events.of(PartsShortage.class)).isEmpty();
	}

	@Test
	void reportsAShortageAndHoldsNothingWhenAnyPartIsMissing() {
		Part plenty = part("PLENTY", 10, 0);
		Part scarce = part("SCARCE", 1, 0);
		UUID budget = UUID.randomUUID();

		reserve.execute(new ReserveStockUseCase.Command(UUID.randomUUID(), "OS-1", budget,
				List.of(new PartQuantity(plenty.id(), 3), new PartQuantity(scarce.id(), 5))));

		assertThat(reservationOf(budget).status()).isEqualTo(ReservationStatus.WAITING);
		assertThat(parts.store.get(plenty.id()).reserved()).isZero();
		assertThat(parts.store.get(scarce.id()).reserved()).isZero();
		assertThat(events.of(PartsShortage.class)).singleElement().satisfies(e -> {
			assertThat(e.budgetId()).isEqualTo(budget);
			assertThat(e.shortages()).singleElement().satisfies(s -> {
				assertThat(s.partId()).isEqualTo(scarce.id());
				assertThat(s.requested()).isEqualTo(5);
				assertThat(s.available()).isEqualTo(1);
			});
		});
		assertThat(events.of(PartsReserved.class)).isEmpty();
	}

	@Test
	void anInactiveOrUnknownPartCountsAsAShortage() {
		Part retired = part("OLD", 10, 0);
		retired.deactivate(clock.instant());
		parts.save(retired);

		request(UUID.randomUUID(), retired, 1);

		assertThat(events.of(PartsShortage.class)).singleElement().satisfies(e -> assertThat(e.shortages().getFirst().available()).isZero());
		UUID ghost = UUID.randomUUID();
		reserve.execute(new ReserveStockUseCase.Command(UUID.randomUUID(), "OS-2", UUID.randomUUID(), List.of(new PartQuantity(ghost, 1))));
		assertThat(events.of(PartsShortage.class)).hasSize(2);
	}

	@Test
	void requestingTheSameBudgetTwiceIsIdempotent() {
		Part filter = part("FLT", 10, 0);
		UUID workOrder = UUID.randomUUID();
		UUID budget = UUID.randomUUID();
		var command = new ReserveStockUseCase.Command(workOrder, "OS-1", budget, List.of(new PartQuantity(filter.id(), 4)));

		reserve.execute(command);
		reserve.execute(command);

		assertThat(store.store).hasSize(1);
		assertThat(parts.store.get(filter.id()).reserved()).isEqualTo(4);
		assertThat(events.of(PartsReserved.class)).hasSize(1);
	}

	@Test
	void aBudgetWithoutPartsReservesNothing() {
		reserve.execute(new ReserveStockUseCase.Command(UUID.randomUUID(), "OS-1", UUID.randomUUID(), List.of()));

		assertThat(store.store).isEmpty();
		assertThat(events.events()).isEmpty();
	}

	@Test
	void aStockEntryHealsWaitingReservationsInArrivalOrder() {
		Part filter = part("FLT", 0, 0);
		UUID first = request(UUID.randomUUID(), filter, 3);
		UUID second = request(UUID.randomUUID(), filter, 3);
		assertThat(events.of(PartsShortage.class)).hasSize(2);

		entry.execute(new RegisterStockEntryUseCase.Command(filter.id(), 4, "invoice", "u"));

		assertThat(reservationOf(first).status()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(reservationOf(second).status()).isEqualTo(ReservationStatus.WAITING);
		assertThat(events.of(PartsReserved.class)).singleElement().extracting(PartsReserved::budgetId).isEqualTo(first);

		entry.execute(new RegisterStockEntryUseCase.Command(filter.id(), 2, "invoice", "u"));

		assertThat(reservationOf(second).status()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(parts.store.get(filter.id()).available()).isZero();
		assertThat(events.of(PartsShortage.class)).hasSize(2);
	}

	@Test
	void aSmallerLaterReservationDoesNotWaitBehindAnOlderOneThatCannotBeSatisfied() {
		Part filter = part("FLT", 0, 0);
		UUID big = request(UUID.randomUUID(), filter, 5);
		UUID small = request(UUID.randomUUID(), filter, 1);

		entry.execute(new RegisterStockEntryUseCase.Command(filter.id(), 2, null, "u"));

		assertThat(reservationOf(big).status()).isEqualTo(ReservationStatus.WAITING);
		assertThat(reservationOf(small).status()).isEqualTo(ReservationStatus.RESERVED);
	}

	@Test
	void aPositiveAdjustmentHealsButANegativeOneDoesNot() {
		Part filter = part("FLT", 1, 0);
		UUID waiting = request(UUID.randomUUID(), filter, 3);

		adjust.execute(new AdjustStockUseCase.Command(filter.id(), -1, "lost", "u"));
		assertThat(reservationOf(waiting).status()).isEqualTo(ReservationStatus.WAITING);

		adjust.execute(new AdjustStockUseCase.Command(filter.id(), 3, "found", "u"));
		assertThat(reservationOf(waiting).status()).isEqualTo(ReservationStatus.RESERVED);
	}

	@Test
	void consumingTakesTheReservedStockOutAndAuditsIt() {
		Part filter = part("FLT", 10, 0);
		UUID workOrder = UUID.randomUUID();
		UUID budget = request(workOrder, filter, 4);

		consume.execute(workOrder);

		Part after = parts.store.get(filter.id());
		assertThat(after.onHand()).isEqualTo(6);
		assertThat(after.reserved()).isZero();
		assertThat(reservationOf(budget).status()).isEqualTo(ReservationStatus.CONSUMED);
		assertThat(movements.store.getLast()).satisfies(m -> {
			assertThat(m.type()).isEqualTo(MovementType.CONSUMPTION);
			assertThat(m.quantity()).isEqualTo(-4);
			assertThat(m.reason()).startsWith("Work order OS-");
			assertThat(m.performedBy()).isEqualTo("system");
		});
		assertThat(events.of(ReservationConsumed.class)).singleElement().extracting(ReservationConsumed::workOrderId).isEqualTo(workOrder);
	}

	@Test
	void consumingTwiceOrWithoutReservationsDoesNothing() {
		Part filter = part("FLT", 10, 0);
		UUID workOrder = UUID.randomUUID();
		request(workOrder, filter, 4);

		consume.execute(workOrder);
		consume.execute(workOrder);
		consume.execute(UUID.randomUUID());

		assertThat(parts.store.get(filter.id()).onHand()).isEqualTo(6);
		assertThat(events.of(ReservationConsumed.class)).hasSize(1);
	}

	@Test
	void consumingLeavesAWaitingReservationUntouched() {
		Part filter = part("FLT", 1, 0);
		UUID workOrder = UUID.randomUUID();
		UUID waiting = request(workOrder, filter, 5);

		consume.execute(workOrder);

		assertThat(reservationOf(waiting).status()).isEqualTo(ReservationStatus.WAITING);
		assertThat(parts.store.get(filter.id()).onHand()).isEqualTo(1);
	}

	@Test
	void releasingFreesTheStockAndHealsTheNextInLine() {
		Part filter = part("FLT", 4, 0);
		UUID holder = UUID.randomUUID();
		UUID held = request(holder, filter, 4);
		UUID queued = request(UUID.randomUUID(), filter, 3);
		assertThat(reservationOf(queued).status()).isEqualTo(ReservationStatus.WAITING);

		release.execute(holder);

		assertThat(reservationOf(held).status()).isEqualTo(ReservationStatus.RELEASED);
		assertThat(reservationOf(queued).status()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(parts.store.get(filter.id()).reserved()).isEqualTo(3);
		assertThat(events.of(ReservationReleased.class)).singleElement().extracting(ReservationReleased::workOrderId).isEqualTo(holder);
	}

	@Test
	void releasingAWaitingReservationHoldsNothingToGiveBack() {
		Part filter = part("FLT", 1, 0);
		UUID workOrder = UUID.randomUUID();
		UUID waiting = request(workOrder, filter, 5);

		release.execute(workOrder);
		release.execute(workOrder);
		release.execute(UUID.randomUUID());

		assertThat(reservationOf(waiting).status()).isEqualTo(ReservationStatus.RELEASED);
		assertThat(parts.store.get(filter.id()).reserved()).isZero();
		assertThat(events.of(ReservationReleased.class)).hasSize(1);
	}

	@Test
	void reservingBelowTheMinimumRaisesLowStockOnce() {
		Part filter = part("FLT", 10, 5);

		request(UUID.randomUUID(), filter, 6);
		request(UUID.randomUUID(), filter, 1);

		assertThat(events.of(LowStockDetected.class)).singleElement().satisfies(e -> {
			assertThat(e.available()).isEqualTo(4);
			assertThat(e.minimumQuantity()).isEqualTo(5);
		});
	}
}
