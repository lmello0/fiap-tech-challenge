package com.fiap.workshop.notification.application.port.in;

import java.util.UUID;

public interface NotifyStatusChangedUseCase {

	void execute(Command command);

	record Command(UUID eventId, String customerName, String customerEmail, String workOrderNumber, String status) {
	}
}
