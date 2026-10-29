package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.workorder.application.port.in.ApproveBudgetUseCase;
import com.fiap.workshop.workorder.domain.Budget;
import com.fiap.workshop.workorder.domain.Decision;
import com.fiap.workshop.workorder.domain.DecisionChannel;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ApproveBudgetService implements ApproveBudgetUseCase {

	private final WorkOrderSupport support;

	ApproveBudgetService(WorkOrderSupport support) {
		this.support = support;
	}

	@Override
	@Transactional
	public WorkOrder execute(Command command) {
		WorkOrder workOrder = support.loadOwned(command.workOrderId(), command.customerScope());
		DecisionChannel channel = DecisionChannels.resolve(command.customerScope(), command.actor(), command.channel());
		int before = workOrder.history().size();
		Instant now = support.clock().instant();
		Budget budget = workOrder.approveBudget(command.budgetId(), new Decision(command.actor(), channel, null, now), now);
		WorkOrder saved = support.save(workOrder, before);
		support.publish(WorkOrderEvents.budgetApproved(saved, budget, now));
		return saved;
	}
}
