package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.workorder.application.port.in.ProposeAdditionalBudgetUseCase;
import com.fiap.workshop.workorder.domain.Budget;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProposeAdditionalBudgetService implements ProposeAdditionalBudgetUseCase {

	private final WorkOrderSupport support;
	private final ItemResolver items;

	ProposeAdditionalBudgetService(WorkOrderSupport support, ItemResolver items) {
		this.support = support;
		this.items = items;
	}

	@Override
	@Transactional
	public WorkOrder execute(Command command) {
		WorkOrder workOrder = support.load(command.workOrderId());
		int before = workOrder.history().size();
		Instant now = support.clock().instant();
		Budget budget = workOrder.proposeAdditionalBudget(items.resolveAll(command.items()), command.actor(), now);
		WorkOrder saved = support.save(workOrder, before);
		support.publish(WorkOrderEvents.budgetIssued(saved, budget, now));
		return saved;
	}
}
