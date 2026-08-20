package com.fiap.techchallenge.history.api.representation;

import com.fiap.techchallenge.shared.audit.ActorType;

import java.time.Instant;
import java.util.UUID;

/** One row of a Timeline, without its Snapshot — see {@code HistorySnapshotInfo} for that. */
public record HistoryEntryInfo(
        UUID id,
        String aggregateType,
        UUID aggregateId,
        String entityType,
        UUID entityId,
        String eventType,
        Instant occurredAt,
        ActorType actorType,
        UUID actorId,
        String actorLabel
) {
}
