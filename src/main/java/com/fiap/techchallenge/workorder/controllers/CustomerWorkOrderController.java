package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.history.api.HistoryQueryService;
import com.fiap.techchallenge.history.api.representation.HistoryEntryInfo;
import com.fiap.techchallenge.history.api.representation.HistorySnapshotInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.workorder.api.BudgetService;
import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.RefuseWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.events.WorkOrderAggregate;
import com.fiap.techchallenge.workorder.api.representation.BudgetInfo;
import com.fiap.techchallenge.workorder.api.representation.CustomerWorkOrderView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CUSTOMER can never create or list/read the WorkOrder resource itself — only read their own work
 * order's Budget and high-level status through this narrower, customer-scoped representation, and
 * approve/refuse a sent Budget.
 */
@RestController
@RequestMapping("work-orders/{id}/customer-view")
@RequiredArgsConstructor
public class CustomerWorkOrderController {

    private final WorkOrderService workOrderService;
    private final BudgetService budgetService;
    private final HistoryQueryService historyQueryService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerWorkOrderView> getForCustomer(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        CustomerWorkOrderView view = workOrderService.getForCustomer(id, UUID.fromString(authentication.getName()));

        return ResponseEntity.ok(view);
    }

    @PostMapping("/budget/approval")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BudgetInfo> approve(
            @PathVariable UUID id,
            @RequestParam UUID budgetId,
            Authentication authentication
    ) {
        BudgetInfo budget = budgetService.approve(budgetId, UUID.fromString(authentication.getName()));

        return ResponseEntity.ok(budget);
    }

    @PostMapping("/budget/refusal")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BudgetInfo> refuse(
            @PathVariable UUID id,
            @RequestParam UUID budgetId,
            @Valid @RequestBody RefuseWorkOrderCommand command,
            Authentication authentication
    ) {
        BudgetInfo budget = budgetService.refuse(budgetId, UUID.fromString(authentication.getName()), command);

        return ResponseEntity.ok(budget);
    }

    /** Only entries the owning module marked customer-visible (CONTEXT.md), and only for a work
     * order this customer actually owns — {@code getForCustomer} throws 404 otherwise. */
    @GetMapping("/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PageResponse<HistoryEntryInfo>> history(
            @PathVariable UUID id,
            @PageableDefault Pageable pageable,
            Authentication authentication
    ) {
        workOrderService.getForCustomer(id, UUID.fromString(authentication.getName()));

        Page<HistoryEntryInfo> page = historyQueryService.customerVisibleTimeline(WorkOrderAggregate.TYPE, id, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/history/{entryId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<HistorySnapshotInfo> historyEntry(
            @PathVariable UUID id,
            @PathVariable UUID entryId,
            Authentication authentication
    ) {
        workOrderService.getForCustomer(id, UUID.fromString(authentication.getName()));

        return historyQueryService
                .customerVisibleSnapshot(entryId, WorkOrderAggregate.TYPE, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
