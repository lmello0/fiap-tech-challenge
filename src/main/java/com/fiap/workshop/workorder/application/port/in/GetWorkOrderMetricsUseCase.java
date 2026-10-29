package com.fiap.workshop.workorder.application.port.in;

import com.fiap.workshop.workorder.application.port.out.WorkOrderMetrics;
import java.time.Instant;

public interface GetWorkOrderMetricsUseCase {

	WorkOrderMetrics.Result execute(Instant from, Instant to);
}
