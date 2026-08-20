package com.fiap.techchallenge.shared.audit;

import com.fiap.techchallenge.shared.logging.LogContext;

import java.time.Instant;
import java.util.UUID;

/**
 * Carried by every {@link DomainEvent}. {@code actorType} distinguishes "the system did it" from
 * "we don't know who did it" — {@code actorId} is only ever set when {@code actorType} is
 * {@link ActorType#USER}; a {@link ActorType#SYSTEM} actor carries the job or process name in
 * {@code actorLabel} instead (history/ADR 0011, CONTEXT.md "Actor").
 *
 * <p>{@code requestId} is a separate concern from the above (ADR 0017, not History) — it's the id
 * of whichever HTTP request, listener invocation, or scheduled job was open on this thread when
 * {@link ActorResolver} built this metadata, carried here so a listener dispatched later, possibly
 * after a restart, can still recover it. {@code null} only if metadata was built outside any unit
 * of work {@link LogContext} knows about.
 */
public record EventMetadata(
        UUID eventId,
        Instant occurredAt,
        ActorType actorType,
        UUID actorId,
        String actorLabel,
        boolean customerVisible,
        String requestId
) {

    public static EventMetadata system(String jobName, boolean customerVisible) {
        return new EventMetadata(
                UUID.randomUUID(), Instant.now(), ActorType.SYSTEM, null, jobName, customerVisible,
                LogContext.requestId());
    }
}
