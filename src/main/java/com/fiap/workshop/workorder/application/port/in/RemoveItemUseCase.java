package com.fiap.workshop.workorder.application.port.in;

import com.fiap.workshop.workorder.domain.WorkOrder;
import java.util.UUID;

public interface RemoveItemUseCase {

	WorkOrder execute(UUID workOrderId, UUID itemId);
}
