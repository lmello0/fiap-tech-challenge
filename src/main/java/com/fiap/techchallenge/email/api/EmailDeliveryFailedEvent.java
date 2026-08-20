package com.fiap.techchallenge.email.api;

import java.util.UUID;

/** Published when sending an {@link EmailRequestedEvent} with a non-null {@code correlationId} fails. */
public record EmailDeliveryFailedEvent(UUID correlationId, String reason) {
}
