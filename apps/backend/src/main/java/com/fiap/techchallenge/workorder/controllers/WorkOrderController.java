package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.history.api.HistoryQueryService;
import com.fiap.techchallenge.history.api.representation.HistoryEntryInfo;
import com.fiap.techchallenge.history.api.representation.HistorySnapshotInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.CreateWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.commands.FinishDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.commands.StartDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.events.WorkOrderAggregate;
import com.fiap.techchallenge.workorder.api.queries.WorkOrderFilterQuery;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderCountStatusInfo;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Staff-facing work order endpoints. CUSTOMER has no access here at all — see
 * {@link CustomerWorkOrderController} for the narrow customer-scoped read and approve/refuse actions.
 * Full endpoint documentation lives on {@link WorkOrderControllerSwaggerDoc}.
 */
@RestController
@RequiredArgsConstructor
public class WorkOrderController implements WorkOrderControllerSwaggerDoc {

    private static final String STAFF = "hasAnyRole('ATTENDANT', 'MECHANIC', 'MANAGER')";

    private final WorkOrderService service;
    private final HistoryQueryService historyQueryService;

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<WorkOrderCountStatusInfo> getWorkOrderStatusInfo(Instant start, Instant end) {
        return ResponseEntity.ok(service.getWorkOrderStatusInfo(start, end));
    }

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<PageResponse<WorkOrderInfo>> getAll(Pageable pageable, WorkOrderFilterQuery filter) {
        Page<WorkOrderInfo> page = service
                .getAllWorkOrders(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<WorkOrderInfo> getById(UUID id) {
        WorkOrderInfo wo = service.getWorkOrderById(id);

        return ResponseEntity.ok(wo);
    }

    @Override
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> create(CreateWorkOrderCommand woCommand) {
        WorkOrderInfo wo = service.create(woCommand);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(wo.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(wo);
    }

    @Override
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> requestDiagnostics(UUID id) {
        WorkOrderInfo wo = service.requestDiagnostics(id);

        return ResponseEntity.ok(wo);
    }

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> startDiagnostics(UUID id, StartDiagnosticsCommand command) {
        WorkOrderInfo wo = service.startDiagnostics(id, command);

        return ResponseEntity.ok(wo);
    }

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> finishDiagnostics(UUID id, FinishDiagnosticsCommand command) {
        WorkOrderInfo wo = service.finishDiagnostics(id, command);

        return ResponseEntity.ok(wo);
    }

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> start(UUID id) {
        WorkOrderInfo wo = service.startService(id);

        return ResponseEntity.ok(wo);
    }

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> finish(UUID id) {
        WorkOrderInfo wo = service.finish(id);

        return ResponseEntity.ok(wo);
    }

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> startLine(UUID id, UUID lineId) {
        WorkOrderInfo wo = service.startLine(id, lineId);

        return ResponseEntity.ok(wo);
    }

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> finishLine(UUID id, UUID lineId) {
        WorkOrderInfo wo = service.finishLine(id, lineId);

        return ResponseEntity.ok(wo);
    }

    @Override
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> pickupReady(UUID id) {
        WorkOrderInfo wo = service.waitingPickup(id);

        return ResponseEntity.ok(wo);
    }

    @Override
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> delivery(UUID id) {
        WorkOrderInfo wo = service.deliver(id);

        return ResponseEntity.ok(wo);
    }

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<PageResponse<HistoryEntryInfo>> history(UUID id, Pageable pageable) {
        Page<HistoryEntryInfo> page = historyQueryService.timeline(WorkOrderAggregate.TYPE, id, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<HistorySnapshotInfo> historyEntry(UUID id, UUID entryId) {
        return historyQueryService
                .snapshot(entryId, WorkOrderAggregate.TYPE, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
