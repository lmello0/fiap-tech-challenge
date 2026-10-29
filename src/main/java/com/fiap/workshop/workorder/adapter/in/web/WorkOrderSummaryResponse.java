package com.fiap.workshop.workorder.adapter.in.web;

import com.fiap.workshop.workorder.application.port.out.WorkOrderSummary;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import java.time.Instant;
import java.util.UUID;

record WorkOrderSummaryResponse(UUID id, String number, WorkOrderStatus status, UUID customerId, String customerName,
		String vehiclePlate, String vehicleDescription, Instant createdAt, Instant updatedAt) {

	static WorkOrderSummaryResponse from(WorkOrderSummary s) {
		return new WorkOrderSummaryResponse(s.id(), s.number(), s.status(), s.customerId(), s.customerName(), s.vehiclePlate(),
				s.vehicleDescription(), s.createdAt(), s.updatedAt());
	}
}
