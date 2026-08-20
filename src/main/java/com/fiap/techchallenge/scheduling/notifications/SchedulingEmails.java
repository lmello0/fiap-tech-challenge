package com.fiap.techchallenge.scheduling.notifications;

import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import com.fiap.techchallenge.scheduling.properties.SchedulingEmailProperties;
import com.fiap.techchallenge.scheduling.properties.SchedulingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Scheduling's facade onto the email module (ADR 0004) — mirrors {@code auth.notifications.AuthEmails}
 * exactly: it never sends anything, only publishes {@link EmailRequestedEvent}; links point at SPA
 * routes, not the API, and are always {@code POST}-consumed so a mail scanner following the link can
 * never itself act on a single-use token.
 */
@Component
@RequiredArgsConstructor
public class SchedulingEmails {

    private final ApplicationEventPublisher events;
    private final SchedulingEmailProperties properties;
    private final SchedulingProperties schedulingProperties;

    public void dropoffBookingConfirmed(String toEmail, String accessToken, String registrationToken) {
        publish(toEmail, "Your drop-off visit is booked", null, """
                Your drop-off visit at the shop is confirmed.

                Manage this booking (view, cancel, or reschedule):
                %s

                Want to skip the form next time and track your repair online? Finish setting up your account:
                %s
                """.formatted(link("/appointments/manage", accessToken), link("/appointments/complete-registration", registrationToken)));
    }

    public void appointmentCancelled(String toEmail, String reasonMessage) {
        publish(toEmail, "Your appointment was cancelled", null, """
                Your appointment at the shop has been cancelled.
                %s
                """.formatted(reasonMessage == null || reasonMessage.isBlank() ? "" : "\n" + reasonMessage));
    }

    public void pickupInvitation(String toEmail, String rawToken) {
        Duration ttl = schedulingProperties.pickupInvitationTokenTtl();

        publish(toEmail, "Your vehicle is ready for pickup", ttl, """
                Great news — your vehicle is ready to be picked up.

                Book a pickup time:
                %s

                This link expires in %s.
                """.formatted(link("/appointments/pickup/book", rawToken), humanize(ttl)));
    }

    private void publish(String toEmail, String subject, Duration ttl, String body) {
        events.publishEvent(EmailRequestedEvent.builder()
                .to(List.of(toEmail))
                .subject(subject)
                .plainText(body)
                .expiresAt(ttl == null ? null : Instant.now().plus(ttl))
                .build());
    }

    private String link(String path, String rawToken) {
        return "%s%s?token=%s".formatted(properties.baseUrl(), path, rawToken);
    }

    private static String humanize(Duration ttl) {
        return ttl.toHours() >= 1
                ? ttl.toHours() + " hour(s)"
                : ttl.toMinutes() + " minute(s)";
    }
}
