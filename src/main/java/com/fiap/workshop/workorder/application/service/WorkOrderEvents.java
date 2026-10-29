package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.UuidV7;
import com.fiap.workshop.workorder.api.events.BudgetApproved;
import com.fiap.workshop.workorder.api.events.BudgetIssued;
import com.fiap.workshop.workorder.api.events.BudgetLineData;
import com.fiap.workshop.workorder.api.events.BudgetRejected;
import com.fiap.workshop.workorder.api.events.PartQuantity;
import com.fiap.workshop.workorder.api.events.WorkOrderCancelled;
import com.fiap.workshop.workorder.api.events.WorkOrderCompleted;
import com.fiap.workshop.workorder.api.events.WorkOrderDelivered;
import com.fiap.workshop.workorder.api.events.WorkOrderOpened;
import com.fiap.workshop.workorder.api.events.WorkOrderStatusChanged;
import com.fiap.workshop.workorder.domain.Budget;
import com.fiap.workshop.workorder.domain.StatusChange;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.time.Instant;
import java.util.List;

final class WorkOrderEvents {

	private WorkOrderEvents() {
	}

	static WorkOrderOpened opened(WorkOrder wo, Instant now) {
		return new WorkOrderOpened(UuidV7.generate(), now, wo.id(), wo.number(), wo.customer().id(), wo.vehicle().id());
	}

	static WorkOrderStatusChanged statusChanged(WorkOrder wo, StatusChange change) {
		return new WorkOrderStatusChanged(UuidV7.generate(), change.occurredAt(), wo.id(), wo.number(), wo.customer().id(),
				change.from() == null ? null : change.from().name(), change.to().name(), change.actor().subject(), change.actor().role());
	}

	static BudgetIssued budgetIssued(WorkOrder wo, Budget budget, Instant now) {
		List<BudgetLineData> lines = budget.lines().stream()
				.map(l -> new BudgetLineData(l.type().name(), l.referenceId(), l.name(), l.unitPrice().amount(), l.quantity()))
				.toList();
		return new BudgetIssued(UuidV7.generate(), now, wo.id(), wo.number(), budget.id(), budget.sequence(), budget.kind().name(),
				budget.total().amount(), wo.customer().id(), wo.customer().name(), wo.customer().email(), lines);
	}

	static BudgetApproved budgetApproved(WorkOrder wo, Budget budget, Instant now) {
		return new BudgetApproved(UuidV7.generate(), now, wo.id(), wo.number(), budget.id(), budget.total().amount(),
				parts(budget.parts()), budget.decision().channel().name());
	}

	static BudgetRejected budgetRejected(WorkOrder wo, Budget budget, Instant now) {
		return new BudgetRejected(UuidV7.generate(), now, wo.id(), wo.number(), budget.id(), budget.decision().reason(),
				budget.decision().channel().name());
	}

	static WorkOrderCompleted completed(WorkOrder wo, Instant now) {
		return new WorkOrderCompleted(UuidV7.generate(), now, wo.id(), wo.number(), wo.customer().id(), wo.customer().name(),
				wo.customer().email(), parts(wo.approvedParts()));
	}

	static WorkOrderDelivered delivered(WorkOrder wo, Instant now) {
		return new WorkOrderDelivered(UuidV7.generate(), now, wo.id(), wo.number(), wo.customer().id());
	}

	static WorkOrderCancelled cancelled(WorkOrder wo, String reason, Instant now) {
		return new WorkOrderCancelled(UuidV7.generate(), now, wo.id(), wo.number(), wo.customer().id(), reason);
	}

	private static List<PartQuantity> parts(List<com.fiap.workshop.workorder.domain.PartQuantity> parts) {
		return parts.stream().map(p -> new PartQuantity(p.partId(), p.quantity())).toList();
	}
}
