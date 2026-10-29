package com.fiap.workshop.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentTest {

	@ParameterizedTest
	@ValueSource(strings = {"529.982.247-25", "52998224725", "111.444.777-35"})
	void acceptsValidCpf(String raw) {
		Document document = Document.of(raw);
		assertThat(document.type()).isEqualTo(Document.Type.CPF);
		assertThat(document.value()).hasSize(11).containsOnlyDigits();
	}

	@ParameterizedTest
	@ValueSource(strings = {"11.222.333/0001-81", "11222333000181", "45.723.174/0001-10"})
	void acceptsValidCnpj(String raw) {
		assertThat(Document.of(raw).type()).isEqualTo(Document.Type.CNPJ);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "123", "529.982.247-24", "111.111.111-11", "11.222.333/0001-80", "00000000000000", "abc"})
	void rejectsInvalid(String raw) {
		assertThatThrownBy(() -> Document.of(raw)).isInstanceOf(DomainException.class);
	}

	@org.junit.jupiter.api.Test
	void rejectsNull() {
		assertThatThrownBy(() -> Document.of(null)).isInstanceOf(DomainException.class);
	}
}
