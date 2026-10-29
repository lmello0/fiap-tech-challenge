package com.fiap.workshop.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.notification.application.port.in.NotifyBudgetIssuedUseCase;
import com.fiap.workshop.notification.application.port.in.NotifyLowStockUseCase;
import com.fiap.workshop.notification.application.port.in.NotifyStatusChangedUseCase;
import com.fiap.workshop.notification.application.port.in.NotifyWorkOrderCompletedUseCase;
import com.fiap.workshop.notification.application.port.out.MailSender;
import com.fiap.workshop.notification.application.port.out.ProcessedEvents;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationServicesTest {

	record Sent(String to, String subject, String template, Map<String, Object> variables) {
	}

	private final List<Sent> sent = new ArrayList<>();
	private final Set<String> seen = new HashSet<>();
	private boolean failing;
	private final MailSender mail = (to, subject, template, variables) -> {
		if (failing) {
			throw new IllegalStateException("smtp down");
		}
		sent.add(new Sent(to, subject, template, variables));
	};
	private final ProcessedEvents processed = (eventId, listener) -> seen.add(eventId + listener);

	private NotifyBudgetIssuedUseCase.Command budget(UUID eventId, String kind) {
		return new NotifyBudgetIssuedUseCase.Command(eventId, "Ana", "ana@example.com", "OS-2026-000001", 2, kind, new BigDecimal("220"),
				List.of(new NotifyBudgetIssuedUseCase.Line("PART", "Oil filter", new BigDecimal("35"), 2)));
	}

	@Test
	void budgetEmailListsTheLinesAndFormatsMoney() {
		new NotifyBudgetIssuedService(processed, mail).execute(budget(UUID.randomUUID(), "INITIAL"));

		assertThat(sent).singleElement().satisfies(s -> {
			assertThat(s.to()).isEqualTo("ana@example.com");
			assertThat(s.subject()).isEqualTo("Budget for work order OS-2026-000001 awaits your approval");
			assertThat(s.template()).isEqualTo("budget-issued");
			assertThat(s.variables()).containsEntry("total", "BRL 220.00").containsEntry("additional", false);
			assertThat((List<?>) s.variables().get("lines")).singleElement()
					.isEqualTo(Map.of("name", "Oil filter", "quantity", "2", "unitPrice", "BRL 35.00", "subtotal", "BRL 70.00"));
		});
	}

	@Test
	void additionalBudgetsAreIdentifiedInTheSubject() {
		new NotifyBudgetIssuedService(processed, mail).execute(budget(UUID.randomUUID(), "ADDITIONAL"));

		assertThat(sent.getFirst().subject()).startsWith("Additional budget");
		assertThat(sent.getFirst().variables()).containsEntry("additional", true);
	}

	@Test
	void theSameEventIsNeverEmailedTwice() {
		UUID eventId = UUID.randomUUID();
		var service = new NotifyBudgetIssuedService(processed, mail);

		service.execute(budget(eventId, "INITIAL"));
		service.execute(budget(eventId, "INITIAL"));

		assertThat(sent).hasSize(1);
	}

	@Test
	void completionEmailGoesToTheCustomer() {
		new NotifyWorkOrderCompletedService(processed, mail)
				.execute(new NotifyWorkOrderCompletedUseCase.Command(UUID.randomUUID(), "Ana", "ana@example.com", "OS-2026-000001"));

		assertThat(sent).singleElement().satisfies(s -> {
			assertThat(s.to()).isEqualTo("ana@example.com");
			assertThat(s.subject()).contains("OS-2026-000001");
			assertThat(s.template()).isEqualTo("work-order-completed");
		});
	}

	@Test
	void lowStockEmailGoesToTheAdministrator() {
		new NotifyLowStockService(processed, mail, () -> "admin@workshop.local")
				.execute(new NotifyLowStockUseCase.Command(UUID.randomUUID(), "FLT-1", "Oil filter", 2, 5));

		assertThat(sent).singleElement().satisfies(s -> {
			assertThat(s.to()).isEqualTo("admin@workshop.local");
			assertThat(s.subject()).isEqualTo("Low stock: Oil filter (FLT-1)");
			assertThat(s.variables()).containsEntry("available", 2).containsEntry("minimumQuantity", 5);
		});
	}

	@Test
	void aFailedSendPropagatesSoTheEventCanBeRetried() {
		failing = true;

		assertThatThrownBy(() -> new NotifyWorkOrderCompletedService(processed, mail)
				.execute(new NotifyWorkOrderCompletedUseCase.Command(UUID.randomUUID(), "Ana", "a@b.co", "OS-1")))
				.isInstanceOf(IllegalStateException.class);
		assertThat(sent).isEmpty();
	}

	private NotifyStatusChangedUseCase.Command status(UUID eventId, String status) {
		return new NotifyStatusChangedUseCase.Command(eventId, "Ana", "ana@example.com", "OS-2026-000001", status);
	}

	@Test
	void everyStatusWithoutADedicatedEmailNotifiesTheCustomer() {
		var service = new NotifyStatusChangedService(processed, mail);

		for (String status : List.of("RECEIVED", "IN_DIAGNOSIS", "WAITING_PARTS", "IN_EXECUTION", "DELIVERED", "CANCELLED")) {
			service.execute(status(UUID.randomUUID(), status));
		}

		assertThat(sent).extracting(Sent::subject).containsExactly(
				"Work order OS-2026-000001: received",
				"Work order OS-2026-000001: diagnosis started",
				"Work order OS-2026-000001: waiting for parts",
				"Work order OS-2026-000001: service in progress",
				"Work order OS-2026-000001: vehicle delivered",
				"Work order OS-2026-000001: cancelled");
		assertThat(sent).allSatisfy(s -> {
			assertThat(s.to()).isEqualTo("ana@example.com");
			assertThat(s.template()).isEqualTo("status-changed");
		});
	}

	@Test
	void statusesThatHaveTheirOwnEmailAreNotSentTwice() {
		var service = new NotifyStatusChangedService(processed, mail);

		service.execute(status(UUID.randomUUID(), "WAITING_APPROVAL"));
		service.execute(status(UUID.randomUUID(), "COMPLETED"));
		service.execute(status(UUID.randomUUID(), "UNKNOWN"));

		assertThat(sent).isEmpty();
	}

	@Test
	void aStatusEventIsEmailedOnlyOnce() {
		UUID eventId = UUID.randomUUID();
		var service = new NotifyStatusChangedService(processed, mail);

		service.execute(status(eventId, "IN_EXECUTION"));
		service.execute(status(eventId, "IN_EXECUTION"));

		assertThat(sent).hasSize(1);
	}
}
