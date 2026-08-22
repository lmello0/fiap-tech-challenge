package com.fiap.techchallenge.history.repositories;

import com.fiap.techchallenge.history.entities.HistoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HistoryEntryRepository extends JpaRepository<HistoryEntry, UUID> {

    Page<HistoryEntry> findByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(
            String aggregateType, UUID aggregateId, Pageable pageable);

    Page<HistoryEntry> findByAggregateTypeAndAggregateIdAndCustomerVisibleTrueOrderByOccurredAtDesc(
            String aggregateType, UUID aggregateId, Pageable pageable);

    Optional<HistoryEntry> findByIdAndAggregateTypeAndAggregateId(UUID id, String aggregateType, UUID aggregateId);

    Optional<HistoryEntry> findByIdAndAggregateTypeAndAggregateIdAndCustomerVisibleTrue(
            UUID id, String aggregateType, UUID aggregateId);
}
