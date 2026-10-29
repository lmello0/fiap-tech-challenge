package com.fiap.workshop.notification.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface NotifyBudgetIssuedUseCase {

	void execute(Command command);

	record Line(String type, String name, BigDecimal unitPrice, int quantity) {
	}

	record Command(UUID eventId, String customerName, String customerEmail, String workOrderNumber, int sequence, String kind,
			BigDecimal total, List<Line> lines) {
	}
}
