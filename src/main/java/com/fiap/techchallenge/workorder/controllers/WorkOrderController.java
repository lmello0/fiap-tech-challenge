package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.CreateWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.commands.FinishDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.commands.StartDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.queries.WorkOrderFilterQuery;
import com.fiap.techchallenge.workorder.api.representation.CustomerWorkOrderView;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Staff-facing work order endpoints. CUSTOMER has no access here at all — see
 * {@link CustomerWorkOrderController} for the narrow customer-scoped read and approve/refuse actions.
 */
@RestController
@RequestMapping("work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private static final String STAFF = "hasAnyRole('ATTENDANT', 'MECHANIC', 'MANAGER')";

    private final WorkOrderService service;

    @GetMapping
    @PreAuthorize(STAFF)
    public ResponseEntity<PageResponse<WorkOrderInfo>> getAll(
            @PageableDefault Pageable pageable,
            @Valid WorkOrderFilterQuery filter
    ) {
        Page<WorkOrderInfo> page = service
                .getAllWorkOrders(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize(STAFF)
    public ResponseEntity<WorkOrderInfo> getById(@PathVariable UUID id) {
        WorkOrderInfo wo = service.getWorkOrderById(id);

        return ResponseEntity.ok(wo);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> create(@Valid @RequestBody CreateWorkOrderCommand woCommand) {
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

    @PostMapping("/{id}/diagnostics/request")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> requestDiagnostics(@PathVariable UUID id) {
        WorkOrderInfo wo = service.requestDiagnostics(id);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/diagnostics/start")
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> startDiagnostics(
            @PathVariable UUID id,
            @Valid @RequestBody StartDiagnosticsCommand command
    ) {
        WorkOrderInfo wo = service.startDiagnostics(id, command);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/diagnostics/finish")
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> finishDiagnostics(
            @PathVariable UUID id,
            @Valid @RequestBody FinishDiagnosticsCommand command
    ) {
        WorkOrderInfo wo = service.finishDiagnostics(id, command);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/service/start")
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> start(@PathVariable UUID id) {
        WorkOrderInfo wo = service.startService(id);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/service/finish")
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> finish(@PathVariable UUID id) {
        WorkOrderInfo wo = service.finish(id);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/lines/{lineId}/start")
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> startLine(@PathVariable UUID id, @PathVariable UUID lineId) {
        WorkOrderInfo wo = service.startLine(id, lineId);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/lines/{lineId}/finish")
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> finishLine(@PathVariable UUID id, @PathVariable UUID lineId) {
        WorkOrderInfo wo = service.finishLine(id, lineId);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/pickup-ready")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> pickupReady(@PathVariable UUID id) {
        WorkOrderInfo wo = service.waitingPickup(id);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/delivery")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<WorkOrderInfo> delivery(@PathVariable UUID id) {
        WorkOrderInfo wo = service.deliver(id);

        return ResponseEntity.ok(wo);
    }
}
