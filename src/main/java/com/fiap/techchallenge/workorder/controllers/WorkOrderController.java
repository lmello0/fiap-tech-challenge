package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.CreateWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.commands.FinishDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.commands.RefuseWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.commands.StartDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.queries.WorkOrderFilterQuery;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService service;

    @GetMapping
    public ResponseEntity<PageResponse<WorkOrderInfo>> getAll(
            @PageableDefault Pageable pageable,
            @Valid WorkOrderFilterQuery filter
    ) {
        Page<WorkOrderInfo> page = service
                .getAllWorkOrders(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderInfo> getById(@PathVariable UUID id) {
        WorkOrderInfo wo = service.getWorkOrderById(id);

        return ResponseEntity.ok(wo);
    }

    @PostMapping
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
    public ResponseEntity<WorkOrderInfo> requestDiagnostics(@PathVariable UUID id) {
        WorkOrderInfo wo = service.requestDiagnostics(id);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/diagnostics/start")
    public ResponseEntity<WorkOrderInfo> startDiagnostics(
            @PathVariable UUID id,
            @Valid @RequestBody StartDiagnosticsCommand command
    ) {
        WorkOrderInfo wo = service.startDiagnostics(id, command);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/diagnostics/finish")
    public ResponseEntity<WorkOrderInfo> finishDiagnostics(
            @PathVariable UUID id,
            @Valid @RequestBody FinishDiagnosticsCommand command
    ) {
        WorkOrderInfo wo = service.finishDiagnostics(id, command);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/approval")
    public ResponseEntity<WorkOrderInfo> approve(@PathVariable UUID id) {
        WorkOrderInfo wo = service.approve(id);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/refusal")
    public ResponseEntity<WorkOrderInfo> refuse(
            @PathVariable UUID id,
            @RequestBody @Valid RefuseWorkOrderCommand command
    ) {
        WorkOrderInfo wo = service.refuse(id, command);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/service/start")
    public ResponseEntity<WorkOrderInfo> start(@PathVariable UUID id) {
        WorkOrderInfo wo = service.startService(id);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/service/finish")
    public ResponseEntity<WorkOrderInfo> finish(@PathVariable UUID id) {
        WorkOrderInfo wo = service.finish(id);

        return ResponseEntity.ok(wo);
    }

    // The rest of this controller has no role gating yet (a pre-existing gap, not introduced here —
    // see the inventory module's write-up). These two are gated because the plan for per-service
    // timing calls for it explicitly: only a MECHANIC actually performs the row's work.
    @PostMapping("/{id}/rows/{rowId}/start")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<WorkOrderInfo> startRow(@PathVariable UUID id, @PathVariable UUID rowId) {
        WorkOrderInfo wo = service.startRow(id, rowId);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/rows/{rowId}/finish")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<WorkOrderInfo> finishRow(@PathVariable UUID id, @PathVariable UUID rowId) {
        WorkOrderInfo wo = service.finishRow(id, rowId);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/pickup-ready")
    public ResponseEntity<WorkOrderInfo> pickupReady(@PathVariable UUID id) {
        WorkOrderInfo wo = service.waitingPickup(id);

        return ResponseEntity.ok(wo);
    }

    @PostMapping("/{id}/delivery")
    public ResponseEntity<WorkOrderInfo> delivery(@PathVariable UUID id) {
        WorkOrderInfo wo = service.deliver(id);

        return ResponseEntity.ok(wo);
    }


}
