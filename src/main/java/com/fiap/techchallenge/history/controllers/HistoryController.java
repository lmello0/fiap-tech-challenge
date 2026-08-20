package com.fiap.techchallenge.history.controllers;

import com.fiap.techchallenge.history.api.HistoryQueryService;
import com.fiap.techchallenge.history.api.representation.HistoryEntryInfo;
import com.fiap.techchallenge.history.api.representation.HistorySnapshotInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Generic staff read surface over any aggregate's Timeline — for a customer-scoped, ownership-checked
 * read of one Work Order's history, see {@code CustomerWorkOrderController} instead, which enforces
 * ownership before ever calling {@link HistoryQueryService} (History has no notion of who owns what).
 */
@RestController
@RequestMapping("history/{aggregateType}/{aggregateId}")
@RequiredArgsConstructor
public class HistoryController {

    private static final String STAFF = "hasAnyRole('ATTENDANT', 'MECHANIC', 'MANAGER', 'STOCKIST')";

    private final HistoryQueryService historyQueryService;

    @GetMapping
    @PreAuthorize(STAFF)
    public ResponseEntity<PageResponse<HistoryEntryInfo>> timeline(
            @PathVariable String aggregateType,
            @PathVariable UUID aggregateId,
            @PageableDefault Pageable pageable
    ) {
        Page<HistoryEntryInfo> page = historyQueryService.timeline(aggregateType, aggregateId, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/entries/{entryId}")
    @PreAuthorize(STAFF)
    public ResponseEntity<HistorySnapshotInfo> snapshot(
            @PathVariable String aggregateType,
            @PathVariable UUID aggregateId,
            @PathVariable UUID entryId
    ) {
        return historyQueryService
                .snapshot(entryId, aggregateType, aggregateId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
