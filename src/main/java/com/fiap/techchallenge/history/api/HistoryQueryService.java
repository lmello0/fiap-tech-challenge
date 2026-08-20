package com.fiap.techchallenge.history.api;

import com.fiap.techchallenge.history.api.representation.HistoryEntryInfo;
import com.fiap.techchallenge.history.api.representation.HistorySnapshotInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * The only way anything outside {@code history} can read what it has recorded — there is no write
 * surface here at all (ADR 0011). Every call is scoped to one aggregate's Timeline.
 *
 * <p>The {@code customerVisible*} methods exist because ownership scoping alone doesn't redact
 * entries or snapshot fields a customer shouldn't see — that decision belongs to the module owning
 * the aggregate, not to {@code history}, so the caller opts into the customer-scoped read explicitly.
 */
public interface HistoryQueryService {

    Page<HistoryEntryInfo> timeline(String aggregateType, UUID aggregateId, Pageable pageable);

    Page<HistoryEntryInfo> customerVisibleTimeline(String aggregateType, UUID aggregateId, Pageable pageable);

    Optional<HistorySnapshotInfo> snapshot(UUID entryId, String aggregateType, UUID aggregateId);

    Optional<HistorySnapshotInfo> customerVisibleSnapshot(UUID entryId, String aggregateType, UUID aggregateId);
}
