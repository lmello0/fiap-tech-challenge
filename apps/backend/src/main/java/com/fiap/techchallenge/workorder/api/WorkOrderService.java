package com.fiap.techchallenge.workorder.api;

import com.fiap.techchallenge.workorder.api.commands.CreateWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.commands.FinishDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.commands.StartDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.queries.WorkOrderFilterQuery;
import com.fiap.techchallenge.workorder.api.representation.CustomerWorkOrderView;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderCountStatusInfo;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface WorkOrderService {

    WorkOrderCountStatusInfo getWorkOrderStatusInfo(Instant startDate, Instant endDate);

    Page<WorkOrderInfo> getAllWorkOrders(WorkOrderFilterQuery filter, Pageable pageable);

    WorkOrderInfo getWorkOrderById(UUID id);

    /** Narrow, customer-scoped read: high-level status plus the current Budget, nothing internal. */
    CustomerWorkOrderView getForCustomer(UUID workOrderId, UUID customerId);

    WorkOrderInfo create(CreateWorkOrderCommand command);

    WorkOrderInfo requestDiagnostics(UUID id);

    WorkOrderInfo startDiagnostics(UUID workOrderId, StartDiagnosticsCommand command);

    /** Atomically transitions to BUDGET_IN_DRAFT and creates the Budget draft seeded with these lines. */
    WorkOrderInfo finishDiagnostics(UUID workOrderId, FinishDiagnosticsCommand command);

    WorkOrderInfo startService(UUID id);

    /** Marks a budget line as started execution. Only valid while the work order is IN_PROGRESS. */
    WorkOrderInfo startLine(UUID workOrderId, UUID lineId);

    /**
     * Marks a budget line as finished and records how long it took against its service in the
     * inventory catalog, feeding that service's rolling average execution time.
     */
    WorkOrderInfo finishLine(UUID workOrderId, UUID lineId);

    WorkOrderInfo finish(UUID id);

    WorkOrderInfo waitingPickup(UUID id);

    WorkOrderInfo deliver(UUID id);
}
