package com.fiap.techchallenge.shared.audit;

import java.util.UUID;

/**
 * Implemented by every event that {@code history} should record. {@code history} subscribes to this
 * interface alone (ADR 0012) — it never imports a domain module.
 *
 * <p>{@code aggregateType}/{@code aggregateId} name the Timeline this entry belongs to (CONTEXT.md
 * "Timeline") — for an event about a sub-entity such as a Budget Line, that's still the owning Work
 * Order, not the line itself. {@code entityType}/{@code entityId} default to the aggregate and only
 * need overriding when the event is about something narrower than its aggregate.
 *
 * <p>{@code snapshot()} is the aggregate's state at the moment this event was raised, assembled by
 * the emitting module inside its own transaction (ADR 0012) — never derived by {@code history}.
 */
public interface DomainEvent {

    /** A stable code namespaced by aggregate, e.g. {@code "WORK_ORDER_APPROVED"} — never a class name (ADR 0012). */
    String eventType();

    String aggregateType();

    UUID aggregateId();

    default String entityType() {
        return aggregateType();
    }

    default UUID entityId() {
        return aggregateId();
    }

    EventMetadata metadata();

    /** The aggregate's full state at this moment, owned and assembled by the emitting module. */
    Object snapshot();

    default int schemaVersion() {
        return 1;
    }
}
