package com.fiap.workshop.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PartTest {

	private static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
	private static final Instant LATER = NOW.plusSeconds(60);

	private Part filter() {
		return Part.create(" flt-001 ", " Oil filter ", " OEM ", Money.of("35"), 5, NOW);
	}

	@Test
	void startsActiveWithNoStockAndNormalizedFields() {
		Part part = filter();

		assertThat(part.sku()).isEqualTo("FLT-001");
		assertThat(part.name()).isEqualTo("Oil filter");
		assertThat(part.onHand()).isZero();
		assertThat(part.active()).isTrue();
		assertThat(part.lowStock()).isTrue();
	}

	@Test
	void rejectsInvalidValues() {
		assertThatThrownBy(() -> Part.create(" ", "A", null, Money.ZERO, 0, NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Part.create("S", " ", null, Money.ZERO, 0, NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Part.create("S", "A", null, Money.of("-1"), 0, NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Part.create("S", "A", null, null, 0, NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Part.create("S", "A", null, Money.ZERO, -1, NOW)).isInstanceOf(DomainException.class);
	}

	@Test
	void receivingStockRecordsAnEntryMovement() {
		Part part = filter();

		StockMovement movement = part.receive(20, " invoice 123 ", "user-1", LATER);

		assertThat(part.onHand()).isEqualTo(20);
		assertThat(movement.type()).isEqualTo(MovementType.ENTRY);
		assertThat(movement.quantity()).isEqualTo(20);
		assertThat(movement.balanceAfter()).isEqualTo(20);
		assertThat(movement.reason()).isEqualTo("invoice 123");
		assertThat(movement.partId()).isEqualTo(part.id());
		assertThat(part.updatedAt()).isEqualTo(LATER);
	}

	@Test
	void receivingRequiresAPositiveQuantity() {
		assertThatThrownBy(() -> filter().receive(0, null, "u", LATER)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> filter().receive(-3, null, "u", LATER)).isInstanceOf(DomainException.class);
	}

	@Test
	void adjustmentsNeedAReasonAndCannotGoBelowZero() {
		Part part = filter();
		part.receive(10, null, "u", NOW);

		StockMovement loss = part.adjust(-4, "broken", "u", LATER);

		assertThat(part.onHand()).isEqualTo(6);
		assertThat(loss.type()).isEqualTo(MovementType.ADJUSTMENT);
		assertThat(loss.balanceAfter()).isEqualTo(6);
		assertThatThrownBy(() -> part.adjust(-7, "too much", "u", LATER)).isInstanceOf(DomainException.class).hasMessageContaining("Insufficient");
		assertThatThrownBy(() -> part.adjust(1, " ", "u", LATER)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> part.adjust(0, "none", "u", LATER)).isInstanceOf(DomainException.class);
		assertThat(part.onHand()).isEqualTo(6);
	}

	@Test
	void detectsCrossingTheMinimumOnlyOnce() {
		Part part = filter();
		part.receive(10, null, "u", NOW);

		int before = part.available();
		part.adjust(-5, "sold", "u", LATER);
		assertThat(part.crossedLowStockFrom(before)).isTrue();

		before = part.available();
		part.adjust(-1, "sold", "u", LATER);
		assertThat(part.crossedLowStockFrom(before)).isFalse();
		assertThat(part.lowStock()).isTrue();
	}

	@Test
	void updateChangesDescriptionPriceAndMinimum() {
		Part part = filter();

		part.update("Oil filter XL", null, Money.of("40"), 8, LATER);

		assertThat(part.name()).isEqualTo("Oil filter XL");
		assertThat(part.unitPrice()).isEqualTo(Money.of("40"));
		assertThat(part.minimumQuantity()).isEqualTo(8);
		assertThat(part.sku()).isEqualTo("FLT-001");
	}

	@Test
	void deactivatedPartIsFrozen() {
		Part part = filter();
		part.deactivate(LATER);

		assertThat(part.active()).isFalse();
		assertThatThrownBy(() -> part.update("A", null, Money.ZERO, 0, LATER)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> part.receive(1, null, "u", LATER)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> part.adjust(1, "x", "u", LATER)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> part.deactivate(LATER)).isInstanceOf(DomainException.class);
	}

	@Test
	void reservingHoldsStockWithoutMovingIt() {
		Part part = filter();
		part.receive(10, null, "u", NOW);

		part.reserve(4, LATER);

		assertThat(part.onHand()).isEqualTo(10);
		assertThat(part.reserved()).isEqualTo(4);
		assertThat(part.available()).isEqualTo(6);
	}

	@Test
	void cannotReserveMoreThanAvailable() {
		Part part = filter();
		part.receive(10, null, "u", NOW);
		part.reserve(8, NOW);

		assertThatThrownBy(() -> part.reserve(3, LATER)).isInstanceOf(DomainException.class).hasMessageContaining("Insufficient available");
		assertThatThrownBy(() -> part.reserve(0, LATER)).isInstanceOf(DomainException.class);
		assertThat(part.reserved()).isEqualTo(8);
	}

	@Test
	void releasingGivesTheStockBack() {
		Part part = filter();
		part.receive(10, null, "u", NOW);
		part.reserve(6, NOW);

		part.release(6, LATER);

		assertThat(part.available()).isEqualTo(10);
		assertThatThrownBy(() -> part.release(1, LATER)).isInstanceOf(DomainException.class);
	}

	@Test
	void consumingRemovesReservedStockAndRecordsAMovement() {
		Part part = filter();
		part.receive(10, null, "u", NOW);
		part.reserve(4, NOW);

		StockMovement movement = part.consume(4, "Work order OS-1", "system", LATER);

		assertThat(part.onHand()).isEqualTo(6);
		assertThat(part.reserved()).isZero();
		assertThat(movement.type()).isEqualTo(MovementType.CONSUMPTION);
		assertThat(movement.quantity()).isEqualTo(-4);
		assertThat(movement.balanceAfter()).isEqualTo(6);
		assertThatThrownBy(() -> part.consume(1, "x", "system", LATER)).isInstanceOf(DomainException.class);
	}

	@Test
	void adjustmentsCannotEatIntoReservedStock() {
		Part part = filter();
		part.receive(10, null, "u", NOW);
		part.reserve(8, NOW);

		assertThatThrownBy(() -> part.adjust(-3, "lost", "u", LATER)).isInstanceOf(DomainException.class).hasMessageContaining("available");
		part.adjust(-2, "lost", "u", LATER);

		assertThat(part.onHand()).isEqualTo(8);
		assertThat(part.available()).isZero();
	}

	@Test
	void lowStockLooksAtAvailableStockNotOnHand() {
		Part part = Part.create("S", "A", null, Money.of("1"), 5, NOW);
		part.receive(10, null, "u", NOW);
		assertThat(part.lowStock()).isFalse();

		int before = part.available();
		part.reserve(6, LATER);

		assertThat(part.lowStock()).isTrue();
		assertThat(part.crossedLowStockFrom(before)).isTrue();
	}
}
