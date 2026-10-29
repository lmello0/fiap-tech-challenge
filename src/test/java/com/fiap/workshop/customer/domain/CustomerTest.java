package com.fiap.workshop.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.shared.domain.Document;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.Email;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CustomerTest {

	private static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
	private static final Instant LATER = NOW.plusSeconds(60);

	private Customer customer() {
		return Customer.register("  Ana Souza ", Document.of("529.982.247-25"), Email.of("ana@example.com"), " 11 99999-0000 ", NOW);
	}

	@Test
	void registersActiveCustomerWithNormalizedFields() {
		Customer customer = customer();

		assertThat(customer.id()).isNotNull();
		assertThat(customer.name()).isEqualTo("Ana Souza");
		assertThat(customer.phone()).isEqualTo("11 99999-0000");
		assertThat(customer.active()).isTrue();
		assertThat(customer.createdAt()).isEqualTo(NOW);
	}

	@Test
	void blankPhoneBecomesNull() {
		Customer customer = Customer.register("Ana", Document.of("52998224725"), Email.of("a@b.co"), "  ", NOW);

		assertThat(customer.phone()).isNull();
	}

	@Test
	void rejectsBlankName() {
		assertThatThrownBy(() -> Customer.register(" ", Document.of("52998224725"), Email.of("a@b.co"), null, NOW))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void updatesMutableFieldsOnly() {
		Customer customer = customer();

		customer.update("Ana Lima", Email.of("lima@example.com"), null, LATER);

		assertThat(customer.name()).isEqualTo("Ana Lima");
		assertThat(customer.email().value()).isEqualTo("lima@example.com");
		assertThat(customer.phone()).isNull();
		assertThat(customer.document().value()).isEqualTo("52998224725");
		assertThat(customer.updatedAt()).isEqualTo(LATER);
	}

	@Test
	void deactivatedCustomerCannotBeChangedOrDeactivatedAgain() {
		Customer customer = customer();
		customer.deactivate(LATER);

		assertThat(customer.active()).isFalse();
		assertThatThrownBy(() -> customer.update("X", Email.of("x@y.co"), null, LATER)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> customer.deactivate(LATER)).isInstanceOf(DomainException.class);
		assertThatThrownBy(customer::requireActive).isInstanceOf(DomainException.class);
	}
}
