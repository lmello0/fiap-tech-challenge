package com.fiap.techchallenge.history.entities;

import com.fiap.techchallenge.shared.audit.ActorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One immutable row on one aggregate's Timeline (CONTEXT.md "History Entry"). Nothing outside
 * {@code history.services} ever constructs or persists one — see {@code HistoryEntryWriter} — and
 * the {@code history.entry} table itself rejects UPDATE/DELETE at the database level (ADR 0011).
 */
@Entity
@Table(name = "entry", schema = "history")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HistoryEntry {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 40, updatable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "entity_type", nullable = false, length = 40, updatable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private UUID entityId;

    @Column(name = "event_type", nullable = false, length = 60, updatable = false)
    private String eventType;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 10, updatable = false)
    private ActorType actorType;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "actor_label", length = 100, updatable = false)
    private String actorLabel;

    @Column(name = "customer_visible", nullable = false, updatable = false)
    private boolean customerVisible;

    @Column(name = "schema_version", nullable = false, updatable = false)
    private int schemaVersion;

    /** Raw JSON text — see ADR 0012 on why {@code history} never deserializes this into a domain type. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", nullable = false, columnDefinition = "jsonb", updatable = false)
    private String snapshot;
}
