package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.workorder.application.port.in.GetWorkOrderMetricsUseCase;
import com.fiap.workshop.workorder.application.port.out.WorkOrderMetrics;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetWorkOrderMetricsService implements GetWorkOrderMetricsUseCase {

	private static final Duration DEFAULT_WINDOW = Duration.ofDays(30);

	private final WorkOrderMetrics metrics;
	private final Clock clock;

	GetWorkOrderMetricsService(WorkOrderMetrics metrics, Clock clock) {
		this.metrics = metrics;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public WorkOrderMetrics.Result execute(Instant from, Instant to) {
		Instant end = to == null ? clock.instant() : to;
		Instant start = from == null ? end.minus(DEFAULT_WINDOW) : from;
		if (!start.isBefore(end)) {
			throw new DomainException("The start of the period must be before its end");
		}
		return metrics.completedBetween(start, end);
	}
}
