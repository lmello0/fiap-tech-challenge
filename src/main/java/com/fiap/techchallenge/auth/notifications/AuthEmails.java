package com.fiap.techchallenge.auth.notifications;

import com.fiap.techchallenge.auth.properties.AuthEmailProperties;
import com.fiap.techchallenge.auth.properties.VerificationProperties;
import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Auth's facade onto the email module: turns a token flow into a message and publishes it. It never
 * sends anything — the email module owns delivery, and reaching it goes through
 * {@link EmailRequestedEvent} only (see ADR 0004).
 *
 * <p>This is also the one place that knows what auth's emails say. Each message expires alongside the
 * token it carries, using the same TTL that stamped the token, so a delayed retry can never deliver a
 * link that is already dead.
 */
@Component
@RequiredArgsConstructor
public class AuthEmails {

    private final ApplicationEventPublisher events;
    private final VerificationProperties verification;
    private final AuthEmailProperties properties;

    public void passwordReset(String toEmail, String rawToken) {
        Duration ttl = verification.passwordResetTTL();

        publish(toEmail, "Reset your password", ttl, """
                We received a request to reset your password.

                Open the link below to choose a new one:
                %s

                Or paste this code into the app: %s

                This link expires in %s. If you didn't ask for it, you can ignore this email.\
                """.formatted(link("/reset-password", rawToken), rawToken, humanize(ttl)));
    }

    public void emailVerification(String toEmail, String rawToken) {
        Duration ttl = verification.emailVerificationTTL();

        publish(toEmail, "Confirm your email address", ttl, """
                Welcome! Confirm your email address to finish setting up your account.

                Open the link below to confirm:
                %s

                Or paste this code into the app: %s

                This link expires in %s.\
                """.formatted(link("/verify-email", rawToken), rawToken, humanize(ttl)));
    }

    public void emailChange(String toEmail, String rawToken) {
        Duration ttl = verification.emailChangeTTL();

        publish(toEmail, "Confirm your new email address", ttl, """
                A request was made to change your account's email address to this one.

                Open the link below to confirm the change:
                %s

                Or paste this code into the app: %s

                This link expires in %s. Until you confirm, your account keeps its current address.\
                """.formatted(link("/confirm-email-change", rawToken), rawToken, humanize(ttl)));
    }

    private void publish(String toEmail, String subject, Duration ttl, String body) {
        events.publishEvent(EmailRequestedEvent.builder()
                .to(List.of(toEmail))
                .subject(subject)
                .plainText(body)
                .expiresAt(Instant.now().plus(ttl))
                .build());
    }

    private String link(String path, String rawToken) {
        // Every path here is an SPA route, not an API one: baseUrl is the frontend's origin. The
        // link only carries the token to a page, which then POSTs it to the matching /auth
        // endpoint. That indirection is what keeps the token safe — a mail scanner or prefetcher
        // following the link reaches a page, and no GET in this system can consume a single-use
        // token.
        //
        // Tokens are URL-safe Base64 by construction, so the raw value is already a legal query
        // parameter — and the copyable code below the link stays byte-identical to it.
        return "%s%s?token=%s".formatted(properties.baseUrl(), path, rawToken);
    }

    private static String humanize(Duration ttl) {
        return ttl.toHours() >= 1
                ? ttl.toHours() + " hour(s)"
                : ttl.toMinutes() + " minute(s)";
    }
}
