package com.fiap.workshop.workorder.application.port.out;

import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkOrderSummary(UUID id, String number, WorkOrderStatus status, UUID customerId, String customerName,
		String vehiclePlate, String vehicleDescription, Instant createdAt, Instant updatedAt) {
}
