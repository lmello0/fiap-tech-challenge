package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.application.port.in.StartExecutionUseCase;
import com.fiap.workshop.workorder.domain.WorkOrder;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class StartExecutionService implements StartExecutionUseCase {

	private final WorkOrderSupport support;

	StartExecutionService(WorkOrderSupport support) {
		this.support = support;
	}

	@Override
	@Transactional
	public void execute(UUID workOrderId, UUID budgetId) {
		WorkOrder workOrder = support.loadForUpdate(workOrderId);
		boolean waiting = workOrder.awaitingStock() || workOrder.status() == WorkOrderStatus.WAITING_PARTS;
		if (!workOrder.isLastBudget(budgetId) || !waiting) {
			return;
		}
		int before = workOrder.history().size();
		workOrder.startExecution(Actor.SYSTEM, support.clock().instant());
		support.save(workOrder, before);
	}
}
