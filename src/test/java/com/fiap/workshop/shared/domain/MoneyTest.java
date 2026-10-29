package com.fiap.workshop.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

	@Test
	void roundsToTwoDecimalPlaces() {
		assertThat(Money.of("10.005").amount()).isEqualByComparingTo("10.01");
		assertThat(Money.of("10").amount().scale()).isEqualTo(2);
	}

	@Test
	void doesArithmetic() {
		assertThat(Money.of("10.50").plus(Money.of("2.25"))).isEqualTo(Money.of("12.75"));
		assertThat(Money.of("10.50").minus(Money.of("0.50"))).isEqualTo(Money.of("10.00"));
		assertThat(Money.of("19.90").times(3)).isEqualTo(Money.of("59.70"));
	}

	@Test
	void comparesAndDetectsNegative() {
		assertThat(Money.of("1").compareTo(Money.of("2"))).isNegative();
		assertThat(Money.of("-0.01").isNegative()).isTrue();
		assertThat(Money.ZERO.isNegative()).isFalse();
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> new Money(null)).isInstanceOf(DomainException.class);
		assertThat(new Money(BigDecimal.ONE).amount()).isEqualByComparingTo("1.00");
	}
}
