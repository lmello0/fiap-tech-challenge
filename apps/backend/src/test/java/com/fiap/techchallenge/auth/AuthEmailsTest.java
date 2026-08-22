package com.fiap.techchallenge.auth;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.notifications.AuthEmails;
import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the seam auth owns: its services hand a token flow to {@link AuthEmails}, and what comes out
 * is an event — never a send. Delivery is the email module's problem, covered by
 * {@code EmailDeliveryIT}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@RecordApplicationEvents
class AuthEmailsTest {

    private static final String EMAIL = "recipient@example.com";
    private static final String TOKEN = "raw-token_with-urlsafe64";

    @Autowired
    AuthEmails authEmails;

    @Autowired
    ApplicationEvents events;

    @Test
    void passwordResetCarriesALinkACopyableTokenAndTheTokensOwnLifetime() {
        Instant before = Instant.now();

        authEmails.passwordReset(EMAIL, TOKEN);

        EmailRequestedEvent event = published();

        assertThat(event.to()).containsExactly(EMAIL);
        assertThat(event.cc()).isEmpty();
        assertThat(event.bcc()).isEmpty();
        assertThat(event.subject()).isEqualTo("Reset your password");
        assertThat(event.html()).isNull();

        // The token rides the link verbatim: URL-safe Base64 needs no escaping, so the link and the
        // copyable code below it carry the same bytes. Reintroducing standard Base64 breaks this.
        assertThat(event.plainText())
                .contains("http://localhost:4200/reset-password?token=" + TOKEN)
                .contains(TOKEN);

        // The email dies with the token it carries: app.auth.verification.password-reset-ttl is 15m.
        assertThat(event.expiresAt())
                .isBetween(before.plus(Duration.ofMinutes(15)), Instant.now().plus(Duration.ofMinutes(15)));
    }

    @Test
    void emailVerificationExpiresWithItsOwnTwentyFourHourToken() {
        Instant before = Instant.now();

        authEmails.emailVerification(EMAIL, TOKEN);

        EmailRequestedEvent event = published();

        assertThat(event.subject()).isEqualTo("Confirm your email address");

        // The full URL, not just the path: an API-shaped path under the SPA's origin is exactly
        // the drift a bare-fragment assertion let through before.
        assertThat(event.plainText()).contains("http://localhost:4200/verify-email?token=" + TOKEN);
        assertThat(event.expiresAt())
                .isBetween(before.plus(Duration.ofHours(24)), Instant.now().plus(Duration.ofHours(24)));
    }

    @Test
    void emailChangeAddressesTheNewMailboxAndExpiresWithItsToken() {
        Instant before = Instant.now();

        authEmails.emailChange(EMAIL, TOKEN);

        EmailRequestedEvent event = published();

        assertThat(event.to()).containsExactly(EMAIL);
        assertThat(event.subject()).isEqualTo("Confirm your new email address");
        assertThat(event.plainText()).contains("http://localhost:4200/confirm-email-change?token=" + TOKEN);
        assertThat(event.expiresAt())
                .isBetween(before.plus(Duration.ofHours(24)), Instant.now().plus(Duration.ofHours(24)));
    }

    private EmailRequestedEvent published() {
        List<EmailRequestedEvent> published = events.stream(EmailRequestedEvent.class).toList();

        assertThat(published).hasSize(1);

        return published.getFirst();
    }
}
