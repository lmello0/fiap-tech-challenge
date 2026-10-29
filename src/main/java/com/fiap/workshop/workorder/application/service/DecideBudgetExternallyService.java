package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.application.port.in.ApproveBudgetUseCase;
import com.fiap.workshop.workorder.application.port.in.DecideBudgetExternallyUseCase;
import com.fiap.workshop.workorder.application.port.in.RejectBudgetUseCase;
import com.fiap.workshop.workorder.application.port.out.ExternalDecisions;
import com.fiap.workshop.workorder.domain.DecisionChannel;
import com.fiap.workshop.workorder.domain.WorkOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DecideBudgetExternallyService implements DecideBudgetExternallyUseCase {

	private final ExternalDecisions received;
	private final WorkOrderSupport support;
	private final ApproveBudgetUseCase approve;
	private final RejectBudgetUseCase reject;

	DecideBudgetExternallyService(ExternalDecisions received, WorkOrderSupport support, ApproveBudgetUseCase approve,
			RejectBudgetUseCase reject) {
		this.received = received;
		this.support = support;
		this.approve = approve;
		this.reject = reject;
	}

	@Override
	@Transactional
	public Result execute(Command command) {
		if (!received.firstTime(command.eventId())) {
			return Result.DUPLICATE;
		}
		WorkOrder workOrder = support.loadByNumber(command.workOrderNumber());
		if (command.decision() == Decision.APPROVED) {
			approve.execute(new ApproveBudgetUseCase.Command(workOrder.id(), command.budgetId(), Actor.EXTERNAL, DecisionChannel.EXTERNAL, null));
		} else {
			reject.execute(new RejectBudgetUseCase.Command(workOrder.id(), command.budgetId(), Actor.EXTERNAL, DecisionChannel.EXTERNAL,
					command.reason(), null));
		}
		return Result.APPLIED;
	}
}
