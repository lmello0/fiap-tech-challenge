package com.fiap.techchallenge.email.api;

import com.fiap.techchallenge.shared.logging.LogContext;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A request to deliver a single email, published by a module's own email facade and consumed only by
 * the email module (see ADR 0004). Modules never send mail themselves — they publish this and move on.
 *
 * <p>Bodies: supply {@code plainText}, {@code html}, or both. Both present sends
 * {@code multipart/alternative}, which is what mail clients expect from HTML mail.
 *
 * <p>{@code expiresAt} bounds how long the message is worth delivering — a password reset email is
 * pointless once its token has expired. {@code null} means the message never goes stale.
 *
 * <p>{@code requestId} (ADR 0017) defaults to whichever unit of work is open when this is built —
 * a producing module's facade never needs to set it, and {@link com.fiap.techchallenge.email.services.EmailDispatcher}
 * (a separate async listener invocation) reopens {@link LogContext} under it, so retries and
 * post-restart redelivery still log under the request that originally asked for this email.
 */
@Builder
public record EmailRequestedEvent(
        List<String> to,
        List<String> cc,
        List<String> bcc,
        String subject,
        String plainText,
        String html,
        Instant expiresAt,
        UUID correlationId,
        String requestId
) {

    public EmailRequestedEvent {
        to = copyOf(to);
        cc = copyOf(cc);
        bcc = copyOf(bcc);
        requestId = requestId == null ? LogContext.requestId() : requestId;

        if (to.isEmpty()) {
            throw new IllegalArgumentException("An email needs at least one recipient");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("An email needs a subject");
        }
        if (isBlank(plainText) && isBlank(html)) {
            throw new IllegalArgumentException("An email needs a plain text body, an HTML body, or both");
        }
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public boolean isMultipart() {
        return !isBlank(plainText) && !isBlank(html);
    }

    private static List<String> copyOf(List<String> recipients) {
        return recipients == null ? List.of() : List.copyOf(recipients);
    }

    private static boolean isBlank(String body) {
        return body == null || body.isBlank();
    }
}
