package com.fiap.workshop.catalog.adapter.in.web;

import com.fiap.workshop.catalog.domain.RepairService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record ServiceResponse(UUID id, String name, String description, BigDecimal price, int estimatedMinutes, boolean active,
		Instant createdAt, Instant updatedAt) {

	static ServiceResponse from(RepairService s) {
		return new ServiceResponse(s.id(), s.name(), s.description(), s.price().amount(), s.estimatedMinutes(), s.active(),
				s.createdAt(), s.updatedAt());
	}
}
