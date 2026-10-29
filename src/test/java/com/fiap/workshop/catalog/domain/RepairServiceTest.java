package com.fiap.workshop.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RepairServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
	private static final Instant LATER = NOW.plusSeconds(60);

	private RepairService oilChange() {
		return RepairService.create("  Oil change ", " Synthetic oil ", Money.of("150"), 45, NOW);
	}

	@Test
	void createsActiveServiceWithNormalizedFields() {
		RepairService service = oilChange();

		assertThat(service.id()).isNotNull();
		assertThat(service.name()).isEqualTo("Oil change");
		assertThat(service.description()).isEqualTo("Synthetic oil");
		assertThat(service.price()).isEqualTo(Money.of("150.00"));
		assertThat(service.active()).isTrue();
	}

	@Test
	void blankDescriptionBecomesNull() {
		assertThat(RepairService.create("Alignment", " ", Money.ZERO, 30, NOW).description()).isNull();
	}

	@Test
	void rejectsInvalidValues() {
		assertThatThrownBy(() -> RepairService.create(" ", null, Money.ZERO, 30, NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> RepairService.create("A", null, Money.of("-1"), 30, NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> RepairService.create("A", null, null, 30, NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> RepairService.create("A", null, Money.ZERO, 0, NOW)).isInstanceOf(DomainException.class);
	}

	@Test
	void updatesAllFields() {
		RepairService service = oilChange();

		service.update("Full service", null, Money.of("300"), 90, LATER);

		assertThat(service.name()).isEqualTo("Full service");
		assertThat(service.price()).isEqualTo(Money.of("300"));
		assertThat(service.estimatedMinutes()).isEqualTo(90);
		assertThat(service.updatedAt()).isEqualTo(LATER);
	}

	@Test
	void deactivatedServiceCannotBeChanged() {
		RepairService service = oilChange();
		service.deactivate(LATER);

		assertThat(service.active()).isFalse();
		assertThatThrownBy(() -> service.update("A", null, Money.ZERO, 10, LATER)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> service.deactivate(LATER)).isInstanceOf(DomainException.class);
	}
}
