package com.fiap.workshop.workorder.application.port.in;

import com.fiap.workshop.workorder.domain.WorkOrder;
import java.util.UUID;

public interface ChangeItemQuantityUseCase {

	WorkOrder execute(Command command);

	record Command(UUID workOrderId, UUID itemId, int quantity) {
	}
}
