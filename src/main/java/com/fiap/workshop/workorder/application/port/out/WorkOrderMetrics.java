package com.fiap.workshop.workorder.application.port.out;

import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface WorkOrderMetrics {

	Result completedBetween(Instant from, Instant to);

	record StatusTime(WorkOrderStatus status, long workOrders, Duration average) {
	}

	record Result(long completedWorkOrders, Duration averageExecution, List<StatusTime> byStatus) {
	}
}
