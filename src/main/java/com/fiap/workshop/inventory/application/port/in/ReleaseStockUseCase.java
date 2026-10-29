package com.fiap.workshop.inventory.application.port.in;

import java.util.UUID;

public interface ReleaseStockUseCase {

	void execute(UUID workOrderId);
}
