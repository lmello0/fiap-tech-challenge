package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.application.port.in.WaitForPartsUseCase;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class WaitForPartsService implements WaitForPartsUseCase {

	private final WorkOrderSupport support;

	WaitForPartsService(WorkOrderSupport support) {
		this.support = support;
	}

	@Override
	@Transactional
	public void execute(UUID workOrderId, UUID budgetId) {
		WorkOrder workOrder = support.load(workOrderId);
		if (!workOrder.isLastBudget(budgetId) || !workOrder.awaitingStock()) {
			return;
		}
		int before = workOrder.history().size();
		workOrder.waitForParts(Actor.SYSTEM, support.clock().instant());
		support.save(workOrder, before);
	}
}
