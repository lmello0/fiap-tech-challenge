package com.fiap.techchallenge.email.api;

import java.util.UUID;

/**
 * Published after an {@link EmailRequestedEvent} carrying a non-null {@code correlationId} is sent
 * successfully. Callers that need a delivery-confirmation signal (rather than fire-and-forget) supply
 * a correlation id on the request and listen for this event keyed by it.
 */
public record EmailDeliveredEvent(UUID correlationId) {
}
