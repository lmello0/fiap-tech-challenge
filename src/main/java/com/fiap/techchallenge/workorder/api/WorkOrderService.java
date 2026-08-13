package com.fiap.techchallenge.workorder.api;

import com.fiap.techchallenge.workorder.api.commands.CreateWorkOrderCommand;
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

    WorkOrderInfo approve(UUID id);

    WorkOrderInfo refuse(UUID id, RefuseWorkOrderCommand command);

    WorkOrderInfo startService(UUID id);

    WorkOrderInfo finish(UUID id);

    WorkOrderInfo waitingPickup(UUID id);

    WorkOrderInfo deliver(UUID id);
}
