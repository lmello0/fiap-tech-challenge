package com.fiap.workshop.notification.application.service;

import com.fiap.workshop.notification.application.port.in.NotifyWorkOrderCompletedUseCase;
import com.fiap.workshop.notification.application.port.out.MailSender;
import com.fiap.workshop.notification.application.port.out.ProcessedEvents;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotifyWorkOrderCompletedService implements NotifyWorkOrderCompletedUseCase {

	private final ProcessedEvents processed;
	private final MailSender mail;

	NotifyWorkOrderCompletedService(ProcessedEvents processed, MailSender mail) {
		this.processed = processed;
		this.mail = mail;
	}

	@Override
	@Transactional
	public void execute(Command command) {
		if (!processed.firstTime(command.eventId(), "work-order-completed")) {
			return;
		}
		mail.send(command.customerEmail(), "Your vehicle is ready: work order " + command.workOrderNumber(), "work-order-completed",
				Map.of("customerName", command.customerName(), "workOrderNumber", command.workOrderNumber()));
	}
}
