package com.fiap.techchallenge.history.services;

import com.fiap.techchallenge.history.api.HistoryQueryService;
import com.fiap.techchallenge.history.api.representation.HistoryEntryInfo;
import com.fiap.techchallenge.history.api.representation.HistorySnapshotInfo;
import com.fiap.techchallenge.history.entities.HistoryEntry;
import com.fiap.techchallenge.history.repositories.HistoryEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HistoryQueryServiceImpl implements HistoryQueryService {

    private final HistoryEntryRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<HistoryEntryInfo> timeline(String aggregateType, UUID aggregateId, Pageable pageable) {
        return repository
                .findByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(aggregateType, aggregateId, pageable)
                .map(HistoryQueryServiceImpl::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HistoryEntryInfo> customerVisibleTimeline(String aggregateType, UUID aggregateId, Pageable pageable) {
        return repository
                .findByAggregateTypeAndAggregateIdAndCustomerVisibleTrueOrderByOccurredAtDesc(aggregateType, aggregateId, pageable)
                .map(HistoryQueryServiceImpl::toCustomerInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HistorySnapshotInfo> snapshot(UUID entryId, String aggregateType, UUID aggregateId) {
        return repository
                .findByIdAndAggregateTypeAndAggregateId(entryId, aggregateType, aggregateId)
                .map(this::toSnapshotInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HistorySnapshotInfo> customerVisibleSnapshot(UUID entryId, String aggregateType, UUID aggregateId) {
        return repository
                .findByIdAndAggregateTypeAndAggregateIdAndCustomerVisibleTrue(entryId, aggregateType, aggregateId)
                .map(this::toSnapshotInfo);
    }

    private static HistoryEntryInfo toInfo(HistoryEntry entry) {
        return new HistoryEntryInfo(
                entry.getId(),
                entry.getAggregateType(),
                entry.getAggregateId(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getEventType(),
                entry.getOccurredAt(),
                entry.getActorType(),
                entry.getActorId(),
                entry.getActorLabel()
        );
    }

    /** Ownership scoping says a customer may reach this row at all; it says nothing about who on
     * staff caused it (CONTEXT.md "Actor" — "History distinguishes them", not "History reveals them"). */
    private static HistoryEntryInfo toCustomerInfo(HistoryEntry entry) {
        return new HistoryEntryInfo(
                entry.getId(),
                entry.getAggregateType(),
                entry.getAggregateId(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getEventType(),
                entry.getOccurredAt(),
                entry.getActorType(),
                null,
                null
        );
    }

    private HistorySnapshotInfo toSnapshotInfo(HistoryEntry entry) {
        JsonNode snapshot = objectMapper.readTree(entry.getSnapshot());

        return new HistorySnapshotInfo(
                entry.getId(),
                entry.getEventType(),
                entry.getOccurredAt(),
                entry.getSchemaVersion(),
                snapshot
        );
    }
}
