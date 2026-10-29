package com.fiap.workshop.inventory.application.port.in;

import com.fiap.workshop.inventory.domain.Part;
import java.math.BigDecimal;

public interface CreatePartUseCase {

	Part execute(Command command);

	record Command(String sku, String name, String description, BigDecimal unitPrice, int minimumQuantity) {
	}
}
