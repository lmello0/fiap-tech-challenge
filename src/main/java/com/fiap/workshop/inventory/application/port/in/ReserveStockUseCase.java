package com.fiap.workshop.inventory.application.port.in;

import java.util.List;
import java.util.UUID;

public interface ReserveStockUseCase {

	void execute(Command command);

	record Command(UUID workOrderId, String workOrderNumber, UUID budgetId, List<PartQuantity> parts) {
	}
}
