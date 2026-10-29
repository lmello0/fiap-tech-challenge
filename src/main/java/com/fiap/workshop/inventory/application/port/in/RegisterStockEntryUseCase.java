package com.fiap.workshop.inventory.application.port.in;

import com.fiap.workshop.inventory.domain.StockMovement;
import java.util.UUID;

public interface RegisterStockEntryUseCase {

	StockMovement execute(Command command);

	record Command(UUID partId, int quantity, String note, String performedBy) {
	}
}
