package com.fiap.workshop.notification.application.service;

import com.fiap.workshop.notification.application.port.in.NotifyLowStockUseCase;
import com.fiap.workshop.notification.application.port.out.AdminContact;
import com.fiap.workshop.notification.application.port.out.MailSender;
import com.fiap.workshop.notification.application.port.out.ProcessedEvents;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotifyLowStockService implements NotifyLowStockUseCase {

	private final ProcessedEvents processed;
	private final MailSender mail;
	private final AdminContact admin;

	NotifyLowStockService(ProcessedEvents processed, MailSender mail, AdminContact admin) {
		this.processed = processed;
		this.mail = mail;
		this.admin = admin;
	}

	@Override
	@Transactional
	public void execute(Command command) {
		if (!processed.firstTime(command.eventId(), "low-stock")) {
			return;
		}
		mail.send(admin.email(), "Low stock: " + command.partName() + " (" + command.sku() + ")", "low-stock",
				Map.of("sku", command.sku(), "partName", command.partName(), "available", command.available(),
						"minimumQuantity", command.minimumQuantity()));
	}
}
