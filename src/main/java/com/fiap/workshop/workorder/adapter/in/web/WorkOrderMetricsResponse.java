package com.fiap.workshop.workorder.adapter.in.web;

import com.fiap.workshop.workorder.application.port.out.WorkOrderMetrics;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import java.time.Duration;
import java.util.List;

record WorkOrderMetricsResponse(long completedWorkOrders, long averageExecutionSeconds, String averageExecution,
		List<StatusTimeData> averageTimePerStatus) {

	record StatusTimeData(WorkOrderStatus status, long workOrders, long averageSeconds, String average) {
	}

	static WorkOrderMetricsResponse from(WorkOrderMetrics.Result result) {
		return new WorkOrderMetricsResponse(result.completedWorkOrders(), result.averageExecution().toSeconds(),
				result.averageExecution().toString(),
				result.byStatus().stream().map(s -> new StatusTimeData(s.status(), s.workOrders(), s.average().toSeconds(), format(s.average()))).toList());
	}

	private static String format(Duration duration) {
		return duration.toString();
	}
}
