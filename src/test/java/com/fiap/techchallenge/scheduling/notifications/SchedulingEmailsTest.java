package com.fiap.techchallenge.scheduling.notifications;

import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import com.fiap.techchallenge.scheduling.properties.SchedulingEmailProperties;
import com.fiap.techchallenge.scheduling.properties.SchedulingProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SchedulingEmailsTest {

    private final ApplicationEventPublisher events = mock();
    private final SchedulingEmailProperties emailProperties = new SchedulingEmailProperties("https://app.example.com");

    @Test
    void appointmentCancelledOmitsTheReasonLineWhenNoMessageIsGiven() {
        SchedulingEmails emails = emailsWithPickupTtl(Duration.ofDays(7));

        emails.appointmentCancelled("customer@example.com", null);

        EmailRequestedEvent event = capture();
        assertThat(event.plainText()).doesNotContain("null").contains("Your appointment at the shop has been cancelled.");
    }

    @Test
    void appointmentCancelledIncludesTheReasonLineWhenAMessageIsGiven() {
        SchedulingEmails emails = emailsWithPickupTtl(Duration.ofDays(7));

        emails.appointmentCancelled("customer@example.com", "Staff training day");

        EmailRequestedEvent event = capture();
        assertThat(event.plainText()).contains("Staff training day");
    }

    @Test
    void pickupInvitationHumanizesATtlOfAnHourOrMoreInHours() {
        SchedulingEmails emails = emailsWithPickupTtl(Duration.ofDays(7));

        emails.pickupInvitation("customer@example.com", "raw-token");

        EmailRequestedEvent event = capture();
        assertThat(event.plainText()).contains("168 hour(s)");
    }

    @Test
    void pickupInvitationHumanizesATtlUnderAnHourInMinutes() {
        SchedulingEmails emails = emailsWithPickupTtl(Duration.ofMinutes(30));

        emails.pickupInvitation("customer@example.com", "raw-token");

        EmailRequestedEvent event = capture();
        assertThat(event.plainText()).contains("30 minute(s)");
    }

    private SchedulingEmails emailsWithPickupTtl(Duration pickupInvitationTtl) {
        SchedulingProperties properties = new SchedulingProperties(
                Duration.ofHours(2), Duration.ofDays(30), Duration.ofDays(7), Duration.ofDays(7), pickupInvitationTtl);

        return new SchedulingEmails(events, emailProperties, properties);
    }

    private EmailRequestedEvent capture() {
        ArgumentCaptor<EmailRequestedEvent> captor = ArgumentCaptor.forClass(EmailRequestedEvent.class);
        verify(events).publishEvent(captor.capture());
        return captor.getValue();
    }
}
