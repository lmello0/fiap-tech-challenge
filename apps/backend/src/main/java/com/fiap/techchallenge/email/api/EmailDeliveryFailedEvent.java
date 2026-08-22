package com.fiap.techchallenge.email.api;

import com.fiap.techchallenge.shared.logging.LogContext;

import java.util.UUID;

/**
 * Published when sending an {@link EmailRequestedEvent} with a non-null {@code correlationId} fails.
 *
 * <p>{@code requestId} (ADR 0017) defaults the same way {@link EmailDeliveredEvent}'s does.
 */
public record EmailDeliveryFailedEvent(UUID correlationId, String reason, String requestId) {

    public EmailDeliveryFailedEvent(UUID correlationId, String reason) {
        this(correlationId, reason, null);
    }

    public EmailDeliveryFailedEvent {
        requestId = requestId == null ? LogContext.requestId() : requestId;
    }
}
