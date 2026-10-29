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

		int before = part.onHand();
		part.adjust(-5, "sold", "u", LATER);
		assertThat(part.crossedLowStockFrom(before)).isTrue();

		before = part.onHand();
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
}
