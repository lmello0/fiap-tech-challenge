package com.fiap.workshop.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

	@Test
	void normalizesCaseAndWhitespace() {
		assertThat(Email.of("  John.Doe@Example.COM ").value()).isEqualTo("john.doe@example.com");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "no-at-sign", "a@b", "a b@c.com", "@c.com"})
	void rejectsInvalid(String raw) {
		assertThatThrownBy(() -> Email.of(raw)).isInstanceOf(DomainException.class);
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> Email.of(null)).isInstanceOf(DomainException.class);
	}
}
