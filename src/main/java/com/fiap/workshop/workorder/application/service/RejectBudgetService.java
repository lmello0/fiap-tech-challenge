package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.workorder.application.port.in.RejectBudgetUseCase;
import com.fiap.workshop.workorder.domain.Budget;
import com.fiap.workshop.workorder.domain.Decision;
import com.fiap.workshop.workorder.domain.DecisionChannel;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RejectBudgetService implements RejectBudgetUseCase {

	private final WorkOrderSupport support;

	RejectBudgetService(WorkOrderSupport support) {
		this.support = support;
	}

	@Override
	@Transactional
	public WorkOrder execute(Command command) {
		WorkOrder workOrder = support.loadOwned(command.workOrderId(), command.customerScope());
		DecisionChannel channel = DecisionChannels.resolve(command.customerScope(), command.actor(), command.channel());
		int before = workOrder.history().size();
		Instant now = support.clock().instant();
		String reason = command.reason() == null || command.reason().isBlank() ? null : command.reason().trim();
		Budget budget = workOrder.rejectBudget(command.budgetId(), new Decision(command.actor(), channel, reason, now), now);
		WorkOrder saved = support.save(workOrder, before);
		support.publish(WorkOrderEvents.budgetRejected(saved, budget, now));
		return saved;
	}
}
