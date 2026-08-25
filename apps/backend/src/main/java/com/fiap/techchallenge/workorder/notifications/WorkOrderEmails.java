package com.fiap.techchallenge.workorder.notifications;

import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import com.fiap.techchallenge.workorder.properties.WorkOrderEmailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Work Orders' facade onto the email module (ADR 0004) — mirrors
 * {@code scheduling.notifications.SchedulingEmails} exactly: it never sends anything, only publishes
 * {@link EmailRequestedEvent}; the link points at an SPA route, not the API, and is always
 * POST-consumed so a mail scanner following the link can never itself approve or refuse a Budget.
 */
@Component
@RequiredArgsConstructor
public class WorkOrderEmails {

    private final ApplicationEventPublisher events;
    private final WorkOrderEmailProperties properties;

    public void budgetReadyForApproval(
            String toEmail, UUID budgetId, String orderCode, String vehicleDescription,
            BigDecimal grandTotal, String rawToken
    ) {
        events.publishEvent(EmailRequestedEvent.builder()
                .to(List.of(toEmail))
                .subject("Budget ready for work order " + orderCode)
                .plainText("""
                        Your work order %s (%s) has a budget ready for your review: R$ %s.

                        Approve or refuse it here:
                        %s
                        """.formatted(orderCode, vehicleDescription, formatMoney(grandTotal), link(rawToken)))
                .correlationId(budgetId)
                .build());
    }

    private String link(String rawToken) {
        return "%s/budgets/decide?token=%s".formatted(properties.baseUrl(), rawToken);
    }

    private static String formatMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toString().replace('.', ',');
    }
}
