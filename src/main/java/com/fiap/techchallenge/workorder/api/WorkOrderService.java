package com.fiap.techchallenge.workorder.api;

import com.fiap.techchallenge.workorder.api.commands.CreateWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.commands.FinishDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.commands.RefuseWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.commands.StartDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.queries.WorkOrderFilterQuery;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface WorkOrderService {

    Page<WorkOrderInfo> getAllWorkOrders(WorkOrderFilterQuery filter, Pageable pageable);

    WorkOrderInfo getWorkOrderById(UUID id);

    WorkOrderInfo create(CreateWorkOrderCommand command);

    WorkOrderInfo requestDiagnostics(UUID id);

    WorkOrderInfo startDiagnostics(UUID workOrderId, StartDiagnosticsCommand command);

    WorkOrderInfo finishDiagnostics(UUID workOrderId, FinishDiagnosticsCommand command);

    WorkOrderInfo approve(UUID id);

    WorkOrderInfo refuse(UUID id, RefuseWorkOrderCommand command);

    WorkOrderInfo startService(UUID id);

    /** Marks a SERVICE row as started. Only valid while the work order is IN_PROGRESS. */
    WorkOrderInfo startRow(UUID workOrderId, UUID rowId);

    /**
     * Marks a SERVICE row as finished and records how long it took against the row's service in the
     * inventory catalog, feeding that service's rolling average execution time.
     */
    WorkOrderInfo finishRow(UUID workOrderId, UUID rowId);

    WorkOrderInfo finish(UUID id);

    WorkOrderInfo waitingPickup(UUID id);

    WorkOrderInfo deliver(UUID id);
}
