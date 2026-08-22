package com.fiap.techchallenge.history.api.representation;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * The Snapshot frozen at one History Entry. {@code schemaVersion} says which shape {@code snapshot}
 * was written in — a value produced by code that no longer exists may not have every field a caller
 * expects (ADR 0012).
 */
public record HistorySnapshotInfo(
        UUID id,
        String eventType,
        Instant occurredAt,
        int schemaVersion,
        JsonNode snapshot
) {
}
