package com.fiap.workshop.inventory.application.port.in;

import com.fiap.workshop.inventory.domain.Part;
import java.math.BigDecimal;
import java.util.UUID;

public interface UpdatePartUseCase {

	Part execute(Command command);

	record Command(UUID id, String name, String description, BigDecimal unitPrice, int minimumQuantity) {
	}
}
