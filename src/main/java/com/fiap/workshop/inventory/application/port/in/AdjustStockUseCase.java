package com.fiap.workshop.inventory.application.port.in;

import com.fiap.workshop.inventory.domain.StockMovement;
import java.util.UUID;

public interface AdjustStockUseCase {

	StockMovement execute(Command command);

	record Command(UUID partId, int delta, String reason, String performedBy) {
	}
}
