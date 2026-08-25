package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.history.api.HistoryQueryService;
import com.fiap.techchallenge.history.api.representation.HistoryEntryInfo;
import com.fiap.techchallenge.history.api.representation.HistorySnapshotInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.workorder.api.BudgetService;
import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.RefuseWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.events.WorkOrderAggregate;
import com.fiap.techchallenge.workorder.api.queries.CustomerWorkOrderFilterQuery;
import com.fiap.techchallenge.workorder.api.representation.BudgetInfo;
import com.fiap.techchallenge.workorder.api.representation.CustomerWorkOrderSummary;
import com.fiap.techchallenge.workorder.api.representation.CustomerWorkOrderView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * CUSTOMER can never create or list/read the WorkOrder resource itself — only read their own work
 * order's Budget and high-level status through this narrower, customer-scoped representation, and
 * approve/refuse a sent Budget. Full endpoint documentation lives on
 * {@link CustomerWorkOrderControllerSwaggerDoc}.
 */
@RestController
@RequiredArgsConstructor
public class CustomerWorkOrderController implements CustomerWorkOrderControllerSwaggerDoc {

    private final WorkOrderService workOrderService;
    private final BudgetService budgetService;
    private final HistoryQueryService historyQueryService;

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PageResponse<CustomerWorkOrderSummary>> getMine(Pageable pageable, CustomerWorkOrderFilterQuery filter, Authentication authentication) {
        Page<CustomerWorkOrderSummary> page = workOrderService.getMineForCustomer(UUID.fromString(authentication.getName()), filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerWorkOrderView> getForCustomer(UUID id, Authentication authentication) {
        CustomerWorkOrderView view = workOrderService.getForCustomer(id, UUID.fromString(authentication.getName()));

        return ResponseEntity.ok(view);
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BudgetInfo> approve(UUID id, UUID budgetId, Authentication authentication) {
        BudgetInfo budget = budgetService.approve(budgetId, UUID.fromString(authentication.getName()));

        return ResponseEntity.ok(budget);
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BudgetInfo> refuse(UUID id, UUID budgetId, RefuseWorkOrderCommand command, Authentication authentication) {
        BudgetInfo budget = budgetService.refuse(budgetId, UUID.fromString(authentication.getName()), command);

        return ResponseEntity.ok(budget);
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PageResponse<HistoryEntryInfo>> history(UUID id, Pageable pageable, Authentication authentication) {
        workOrderService.getForCustomer(id, UUID.fromString(authentication.getName()));

        Page<HistoryEntryInfo> page = historyQueryService.customerVisibleTimeline(WorkOrderAggregate.TYPE, id, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<HistorySnapshotInfo> historyEntry(UUID id, UUID entryId, Authentication authentication) {
        workOrderService.getForCustomer(id, UUID.fromString(authentication.getName()));

        return historyQueryService
                .customerVisibleSnapshot(entryId, WorkOrderAggregate.TYPE, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
