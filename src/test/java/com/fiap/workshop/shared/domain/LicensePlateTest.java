package com.fiap.workshop.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LicensePlateTest {

	@ParameterizedTest
	@ValueSource(strings = {"ABC1234", "abc-1234", "ABC1D23", "abc 1d23"})
	void acceptsLegacyAndMercosul(String raw) {
		assertThat(LicensePlate.of(raw).value()).matches("[A-Z0-9]{7}");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "AB1234", "ABCD123", "ABC12345", "1234ABC"})
	void rejectsInvalid(String raw) {
		assertThatThrownBy(() -> LicensePlate.of(raw)).isInstanceOf(DomainException.class);
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> LicensePlate.of(null)).isInstanceOf(DomainException.class);
	}
}
