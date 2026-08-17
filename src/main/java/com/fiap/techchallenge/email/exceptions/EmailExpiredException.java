package com.fiap.techchallenge.email.exceptions;

import com.fiap.techchallenge.email.api.EmailRequestedEvent;

/**
 * Thrown when an {@link EmailRequestedEvent} is picked up
 * after its {@code expiresAt} — the message is no longer worth delivering.
 *
 * <p>It is thrown rather than swallowed on purpose: returning normally would let Spring Modulith mark
 * the publication COMPLETED, which would record an email that was never sent as if it had been.
 * Throwing leaves the publication incomplete, so the staleness monitor marks it FAILED and it stays
 * queryable as "never delivered" until the retention purge collects it (see ADR 0004).
 */
public class EmailExpiredException extends RuntimeException {

    public EmailExpiredException(String message) {
        super(message);
    }
}
