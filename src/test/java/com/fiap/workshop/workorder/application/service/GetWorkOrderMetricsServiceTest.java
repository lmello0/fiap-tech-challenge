package com.fiap.workshop.workorder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.workorder.application.port.out.WorkOrderMetrics;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetWorkOrderMetricsServiceTest {

	private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

	private final Instant[] asked = new Instant[2];
	private final WorkOrderMetrics port = (from, to) -> {
		asked[0] = from;
		asked[1] = to;
		return new WorkOrderMetrics.Result(0, Duration.ZERO, List.of());
	};
	private final GetWorkOrderMetricsService service = new GetWorkOrderMetricsService(port, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void defaultsToTheLastThirtyDays() {
		service.execute(null, null);

		assertThat(asked[1]).isEqualTo(NOW);
		assertThat(asked[0]).isEqualTo(NOW.minus(Duration.ofDays(30)));
	}

	@Test
	void keepsTheRequestedPeriod() {
		Instant from = Instant.parse("2026-01-01T00:00:00Z");
		Instant to = Instant.parse("2026-02-01T00:00:00Z");

		service.execute(from, to);

		assertThat(asked).containsExactly(from, to);
	}

	@Test
	void anOpenEndedStartCountsBackFromTheEnd() {
		Instant to = Instant.parse("2026-02-01T00:00:00Z");

		service.execute(null, to);

		assertThat(asked[0]).isEqualTo(to.minus(Duration.ofDays(30)));
	}

	@Test
	void rejectsAnEmptyOrInvertedPeriod() {
		assertThatThrownBy(() -> service.execute(NOW, NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> service.execute(NOW, NOW.minusSeconds(1))).isInstanceOf(DomainException.class);
	}
}
