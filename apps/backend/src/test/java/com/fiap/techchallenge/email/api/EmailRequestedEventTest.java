package com.fiap.techchallenge.email.api;

import com.fiap.techchallenge.shared.logging.LogContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailRequestedEventTest {

    @Test
    void rejectsAnEmailWithNoRecipients() {
        assertThatThrownBy(() -> event(List.of(), "Subject", "Body").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An email needs at least one recipient");
    }

    @Test
    void rejectsANullSubject() {
        assertThatThrownBy(() -> event(List.of("a@example.com"), null, "Body").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An email needs a subject");
    }

    @Test
    void rejectsABlankSubject() {
        assertThatThrownBy(() -> event(List.of("a@example.com"), "   ", "Body").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An email needs a subject");
    }

    @Test
    void rejectsAnEmailWithNeitherPlainTextNorHtmlBody() {
        assertThatThrownBy(() -> EmailRequestedEvent.builder()
                .to(List.of("a@example.com"))
                .subject("Subject")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An email needs a plain text body, an HTML body, or both");
    }

    @Test
    void nullCcAndBccDefaultToEmptyLists() {
        EmailRequestedEvent built = EmailRequestedEvent.builder()
                .to(List.of("a@example.com"))
                .subject("Subject")
                .plainText("Body")
                .build();

        assertThat(built.cc()).isEmpty();
        assertThat(built.bcc()).isEmpty();
    }

    @Test
    void aNullExpiresAtNeverExpires() {
        EmailRequestedEvent built = event(List.of("a@example.com"), "Subject", "Body").build();

        assertThat(built.isExpired(Instant.now().plus(Duration.ofDays(3650)))).isFalse();
    }

    @Test
    void expiresAtInThePastIsExpired() {
        EmailRequestedEvent built = event(List.of("a@example.com"), "Subject", "Body")
                .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
                .build();

        assertThat(built.isExpired(Instant.now())).isTrue();
    }

    @Test
    void expiresAtExactlyNowIsExpired() {
        Instant now = Instant.now();
        EmailRequestedEvent built = event(List.of("a@example.com"), "Subject", "Body")
                .expiresAt(now)
                .build();

        assertThat(built.isExpired(now)).isTrue();
    }

    @Test
    void expiresAtInTheFutureIsNotExpired() {
        EmailRequestedEvent built = event(List.of("a@example.com"), "Subject", "Body")
                .expiresAt(Instant.now().plus(Duration.ofHours(1)))
                .build();

        assertThat(built.isExpired(Instant.now())).isFalse();
    }

    @Test
    void plainTextOnlyIsNotMultipart() {
        EmailRequestedEvent built = event(List.of("a@example.com"), "Subject", "Body").build();

        assertThat(built.isMultipart()).isFalse();
    }

    @Test
    void htmlOnlyIsNotMultipart() {
        EmailRequestedEvent built = EmailRequestedEvent.builder()
                .to(List.of("a@example.com"))
                .subject("Subject")
                .html("<p>Body</p>")
                .build();

        assertThat(built.isMultipart()).isFalse();
    }

    @Test
    void bothBodiesPresentIsMultipart() {
        EmailRequestedEvent built = EmailRequestedEvent.builder()
                .to(List.of("a@example.com"))
                .subject("Subject")
                .plainText("Text")
                .html("<p>Body</p>")
                .build();

        assertThat(built.isMultipart()).isTrue();
    }

    @Test
    void aNullRequestIdInheritsWhicheverUnitOfWorkIsCurrentlyOpen() {
        try (LogContext.Scope ignored = LogContext.open("the-open-request-id")) {
            EmailRequestedEvent built = event(List.of("a@example.com"), "Subject", "Body").build();

            assertThat(built.requestId()).isEqualTo("the-open-request-id");
        }
    }

    @Test
    void aNullRequestIdStaysNullOutsideAnyOpenUnitOfWork() {
        EmailRequestedEvent built = event(List.of("a@example.com"), "Subject", "Body").build();

        assertThat(built.requestId()).isNull();
    }

    @Test
    void anExplicitRequestIdIsKept() {
        EmailRequestedEvent built = event(List.of("a@example.com"), "Subject", "Body")
                .requestId("explicit-request-id")
                .build();

        assertThat(built.requestId()).isEqualTo("explicit-request-id");
    }

    private static EmailRequestedEvent.EmailRequestedEventBuilder event(
            List<String> to, String subject, String plainText
    ) {
        return EmailRequestedEvent.builder()
                .to(to)
                .subject(subject)
                .plainText(plainText);
    }
}
