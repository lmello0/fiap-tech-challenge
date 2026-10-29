package com.fiap.workshop.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) implements Comparable<Money> {

	public static final Money ZERO = new Money(BigDecimal.ZERO);

	public Money {
		if (amount == null) {
			throw new DomainException("Amount is required");
		}
		amount = amount.setScale(2, RoundingMode.HALF_UP);
	}

	public static Money of(String amount) {
		return new Money(new BigDecimal(amount));
	}

	public Money plus(Money other) {
		return new Money(amount.add(other.amount));
	}

	public Money minus(Money other) {
		return new Money(amount.subtract(other.amount));
	}

	public Money times(long quantity) {
		return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
	}

	public boolean isNegative() {
		return amount.signum() < 0;
	}

	@Override
	public int compareTo(Money other) {
		return amount.compareTo(other.amount);
	}
}
