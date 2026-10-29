package com.fiap.workshop.notification.application.port.in;

import java.util.UUID;

public interface NotifyLowStockUseCase {

	void execute(Command command);

	record Command(UUID eventId, String sku, String partName, int available, int minimumQuantity) {
	}
}
