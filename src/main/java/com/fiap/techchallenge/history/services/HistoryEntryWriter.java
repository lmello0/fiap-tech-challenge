package com.fiap.techchallenge.history.services;

import com.fiap.techchallenge.history.entities.HistoryEntry;
import com.fiap.techchallenge.history.repositories.HistoryEntryRepository;
import com.fiap.techchallenge.shared.audit.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * The only writer {@code history} has. Reacts to {@link DomainEvent} alone — never a concrete event
 * type from another module — so nothing outside this class can ever write a row here (ADR 0011); it
 * imports zero domain types, so adding History for a new module never touches this file (ADR 0012).
 *
 * <p>{@code @ApplicationModuleListener} dispatches after the publishing transaction commits, on an
 * async thread, matching the {@code email} module's pattern.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class HistoryEntryWriter {

    private final HistoryEntryRepository repository;
    private final ObjectMapper objectMapper;

    @ApplicationModuleListener
    void on(DomainEvent event) {
        HistoryEntry entry = new HistoryEntry();
        entry.setAggregateType(event.aggregateType());
        entry.setAggregateId(event.aggregateId());
        entry.setEntityType(event.entityType());
        entry.setEntityId(event.entityId());
        entry.setEventType(event.eventType());
        entry.setOccurredAt(event.metadata().occurredAt());
        entry.setActorType(event.metadata().actorType());
        entry.setActorId(event.metadata().actorId());
        entry.setActorLabel(event.metadata().actorLabel());
        entry.setCustomerVisible(event.metadata().customerVisible());
        entry.setSchemaVersion(event.schemaVersion());
        entry.setSnapshot(objectMapper.writeValueAsString(event.snapshot()));

        repository.save(entry);

        log.debug("Recorded history entry aggregateType={} aggregateId={} eventType={}",
                event.aggregateType(), event.aggregateId(), event.eventType());
    }
}
