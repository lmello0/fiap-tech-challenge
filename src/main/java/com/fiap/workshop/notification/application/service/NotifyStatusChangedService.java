package com.fiap.workshop.notification.application.service;

import com.fiap.workshop.notification.application.port.in.NotifyStatusChangedUseCase;
import com.fiap.workshop.notification.application.port.out.MailSender;
import com.fiap.workshop.notification.application.port.out.ProcessedEvents;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotifyStatusChangedService implements NotifyStatusChangedUseCase {

	private static final Map<String, String> LABELS = Map.of(
			"RECEIVED", "received",
			"IN_DIAGNOSIS", "diagnosis started",
			"WAITING_PARTS", "waiting for parts",
			"IN_EXECUTION", "service in progress",
			"DELIVERED", "vehicle delivered",
			"CANCELLED", "cancelled");

	private final ProcessedEvents processed;
	private final MailSender mail;

	NotifyStatusChangedService(ProcessedEvents processed, MailSender mail) {
		this.processed = processed;
		this.mail = mail;
	}

	@Override
	@Transactional
	public void execute(Command command) {
		String label = LABELS.get(command.status());
		if (label == null || !processed.firstTime(command.eventId(), "status-changed")) {
			return;
		}
		mail.send(command.customerEmail(), "Work order " + command.workOrderNumber() + ": " + label, "status-changed",
				Map.of("customerName", command.customerName(), "workOrderNumber", command.workOrderNumber(), "statusLabel", label));
	}
}
