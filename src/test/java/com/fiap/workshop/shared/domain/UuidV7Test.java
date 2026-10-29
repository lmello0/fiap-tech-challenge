package com.fiap.workshop.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

	@Test
	void hasVersion7AndIetfVariant() {
		UUID id = UuidV7.generate();
		assertThat(id.version()).isEqualTo(7);
		assertThat(id.variant()).isEqualTo(2);
	}

	@Test
	void embedsTheTimestampAndSortsByTime() {
		UUID earlier = UuidV7.generate(1_000_000L);
		UUID later = UuidV7.generate(2_000_000L);

		assertThat(earlier.getMostSignificantBits() >>> 16).isEqualTo(1_000_000L);
		assertThat(earlier.toString()).isLessThan(later.toString());
	}

	@Test
	void generatesDistinctValues() {
		assertThat(UuidV7.generate()).isNotEqualTo(UuidV7.generate());
	}
}
