package com.fiap.workshop.catalog.application.port.in;

import com.fiap.workshop.catalog.domain.RepairService;
import java.math.BigDecimal;
import java.util.UUID;

public interface UpdateServiceUseCase {

	RepairService execute(Command command);

	record Command(UUID id, String name, String description, BigDecimal price, int estimatedMinutes) {
	}
}
