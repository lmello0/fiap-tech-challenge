package com.fiap.workshop.customer.adapter.in.web;

import com.fiap.workshop.customer.domain.Vehicle;
import java.time.Instant;
import java.util.UUID;

record VehicleResponse(UUID id, UUID customerId, String plate, String brand, String model, int year, boolean active,
		Instant createdAt, Instant updatedAt) {

	static VehicleResponse from(Vehicle v) {
		return new VehicleResponse(v.id(), v.customerId(), v.plate().value(), v.brand(), v.model(), v.year(), v.active(),
				v.createdAt(), v.updatedAt());
	}
}
