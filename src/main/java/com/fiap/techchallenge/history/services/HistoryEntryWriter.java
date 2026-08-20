package com.fiap.techchallenge.history.services;

import com.fiap.techchallenge.history.entities.HistoryEntry;
import com.fiap.techchallenge.history.repositories.HistoryEntryRepository;
import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.logging.LogContext;
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
 * async thread, matching the {@code email} module's pattern. {@code requestId} is reopened from the
 * event's {@link com.fiap.techchallenge.shared.audit.EventMetadata} under {@link LogContext} (ADR
 * 0017) purely so this invocation's own canonical line is traceable — it is never written into the
 * {@link HistoryEntry} itself, which stays free of ops/tracing concerns (ADR 0017).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class HistoryEntryWriter {

    private final HistoryEntryRepository repository;
    private final ObjectMapper objectMapper;

    @ApplicationModuleListener
    void on(DomainEvent event) {
        try (LogContext.Scope ignored = LogContext.open(event.metadata().requestId())) {
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

            LogContext.put("aggregateType", event.aggregateType());
            LogContext.put("aggregateId", event.aggregateId());
            LogContext.put("eventType", event.eventType());
            log.info("history_entry_written");
        }
    }
}
