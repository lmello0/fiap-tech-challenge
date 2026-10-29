package com.fiap.workshop.notification.application.service;

import com.fiap.workshop.notification.application.port.in.NotifyBudgetIssuedUseCase;
import com.fiap.workshop.notification.application.port.out.MailSender;
import com.fiap.workshop.notification.application.port.out.ProcessedEvents;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotifyBudgetIssuedService implements NotifyBudgetIssuedUseCase {

	private final ProcessedEvents processed;
	private final MailSender mail;

	NotifyBudgetIssuedService(ProcessedEvents processed, MailSender mail) {
		this.processed = processed;
		this.mail = mail;
	}

	@Override
	@Transactional
	public void execute(Command command) {
		if (!processed.firstTime(command.eventId(), "budget-issued")) {
			return;
		}
		boolean additional = "ADDITIONAL".equals(command.kind());
		List<Map<String, String>> lines = command.lines().stream()
				.map(l -> Map.of("name", l.name(), "quantity", String.valueOf(l.quantity()), "unitPrice", Money.format(l.unitPrice()),
						"subtotal", Money.format(l.unitPrice().multiply(java.math.BigDecimal.valueOf(l.quantity())))))
				.toList();
		String subject = (additional ? "Additional budget " : "Budget ") + "for work order " + command.workOrderNumber() + " awaits your approval";
		mail.send(command.customerEmail(), subject, "budget-issued", Map.of(
				"customerName", command.customerName(),
				"workOrderNumber", command.workOrderNumber(),
				"additional", additional,
				"budgetNumber", command.sequence(),
				"total", Money.format(command.total()),
				"lines", lines));
	}
}
