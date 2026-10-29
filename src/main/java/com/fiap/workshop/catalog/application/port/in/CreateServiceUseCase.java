package com.fiap.workshop.catalog.application.port.in;

import com.fiap.workshop.catalog.domain.RepairService;
import java.math.BigDecimal;

public interface CreateServiceUseCase {

	RepairService execute(Command command);

	record Command(String name, String description, BigDecimal price, int estimatedMinutes) {
	}
}
