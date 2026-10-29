package com.fiap.workshop.workorder.application.port.in;

import java.util.UUID;

public interface WaitForPartsUseCase {

	void execute(UUID workOrderId, UUID budgetId);
}
