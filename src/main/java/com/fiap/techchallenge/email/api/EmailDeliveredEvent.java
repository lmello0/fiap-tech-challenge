package com.fiap.techchallenge.email.api;

import com.fiap.techchallenge.shared.logging.LogContext;

import java.util.UUID;

/**
 * Published after an {@link EmailRequestedEvent} carrying a non-null {@code correlationId} is sent
 * successfully. Callers that need a delivery-confirmation signal (rather than fire-and-forget) supply
 * a correlation id on the request and listen for this event keyed by it.
 *
 * <p>{@code requestId} (ADR 0017) defaults to whatever unit of work is open when this is built —
 * {@link com.fiap.techchallenge.email.services.EmailDispatcher} publishes it from inside the
 * {@link LogContext} scope it reopened under the originating {@link EmailRequestedEvent}'s id, so
 * this carries that same id forward without its one call site needing to pass it explicitly.
 */
public record EmailDeliveredEvent(UUID correlationId, String requestId) {

    public EmailDeliveredEvent(UUID correlationId) {
        this(correlationId, null);
    }

    public EmailDeliveredEvent {
        requestId = requestId == null ? LogContext.requestId() : requestId;
    }
}
