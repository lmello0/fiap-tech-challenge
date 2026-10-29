package com.fiap.workshop.workorder.application.port.in;

import java.util.UUID;

public interface DecideBudgetExternallyUseCase {

	Result execute(Command command);

	enum Decision {
		APPROVED, REJECTED
	}

	enum Result {
		APPLIED, DUPLICATE
	}

	record Command(UUID eventId, String workOrderNumber, UUID budgetId, Decision decision, String reason) {
	}
}
