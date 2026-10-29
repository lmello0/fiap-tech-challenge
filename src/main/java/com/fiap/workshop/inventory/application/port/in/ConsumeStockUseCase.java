package com.fiap.workshop.inventory.application.port.in;

import java.util.UUID;

public interface ConsumeStockUseCase {

	void execute(UUID workOrderId);
}
