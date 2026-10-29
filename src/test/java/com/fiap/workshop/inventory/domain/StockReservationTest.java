package com.fiap.workshop.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.shared.domain.DomainException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StockReservationTest {

	private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");
	private static final Instant LATER = NOW.plusSeconds(60);

	private StockReservation waiting() {
		return StockReservation.request(UUID.randomUUID(), "OS-2026-000001", UUID.randomUUID(),
				List.of(new ReservationLine(UUID.randomUUID(), 2)), NOW);
	}

	@Test
	void startsWaitingWithoutHoldingStock() {
		StockReservation reservation = waiting();

		assertThat(reservation.status()).isEqualTo(ReservationStatus.WAITING);
		assertThat(reservation.holdsStock()).isFalse();
		assertThat(reservation.createdAt()).isEqualTo(NOW);
	}

	@Test
	void needsAtLeastOnePart() {
		assertThatThrownBy(() -> StockReservation.request(UUID.randomUUID(), "OS", UUID.randomUUID(), List.of(), NOW))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void followsReservedThenConsumed() {
		StockReservation reservation = waiting();

		reservation.markReserved(LATER);
		assertThat(reservation.holdsStock()).isTrue();
		reservation.markConsumed(LATER);

		assertThat(reservation.status()).isEqualTo(ReservationStatus.CONSUMED);
		assertThat(reservation.holdsStock()).isFalse();
		assertThat(reservation.updatedAt()).isEqualTo(LATER);
	}

	@Test
	void cannotSkipOrRepeatSteps() {
		StockReservation reservation = waiting();

		assertThatThrownBy(() -> reservation.markConsumed(LATER)).isInstanceOf(DomainException.class);
		reservation.markReserved(LATER);
		assertThatThrownBy(() -> reservation.markReserved(LATER)).isInstanceOf(DomainException.class);
	}

	@Test
	void releaseReportsWhetherStockWasHeld() {
		StockReservation waiting = waiting();
		StockReservation reserved = waiting();
		reserved.markReserved(NOW);

		assertThat(waiting.release(LATER)).isFalse();
		assertThat(reserved.release(LATER)).isTrue();
		assertThat(reserved.status()).isEqualTo(ReservationStatus.RELEASED);
		assertThatThrownBy(() -> reserved.release(LATER)).isInstanceOf(DomainException.class);
	}

	@Test
	void aConsumedReservationCannotBeReleased() {
		StockReservation reservation = waiting();
		reservation.markReserved(NOW);
		reservation.markConsumed(NOW);

		assertThatThrownBy(() -> reservation.release(LATER)).isInstanceOf(DomainException.class);
	}
}
