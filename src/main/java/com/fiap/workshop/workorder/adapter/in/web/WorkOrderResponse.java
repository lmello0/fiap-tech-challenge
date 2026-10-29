package com.fiap.workshop.workorder.adapter.in.web;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.domain.Budget;
import com.fiap.workshop.workorder.domain.BudgetKind;
import com.fiap.workshop.workorder.domain.BudgetStatus;
import com.fiap.workshop.workorder.domain.Decision;
import com.fiap.workshop.workorder.domain.DecisionChannel;
import com.fiap.workshop.workorder.domain.ItemType;
import com.fiap.workshop.workorder.domain.WorkOrder;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record WorkOrderResponse(UUID id, String number, WorkOrderStatus status, CustomerData customer, VehicleData vehicle, String complaint,
		List<ItemData> draftItems, List<BudgetData> budgets, BigDecimal approvedTotal, List<HistoryData> history, Instant createdAt,
		Instant updatedAt) {

	record CustomerData(UUID id, String name, String document, String email) {
	}

	record VehicleData(UUID id, String plate, String brand, String model, int year) {
	}

	record ItemData(UUID id, ItemType type, UUID referenceId, String name, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
	}

	record DecisionData(DecisionChannel channel, String reason, Instant at, ActorData by) {
	}

	record ActorData(String subject, String role) {
	}

	record BudgetData(UUID id, int sequence, BudgetKind kind, BudgetStatus status, UUID previousBudgetId, BigDecimal total,
			Instant issuedAt, List<ItemData> lines, DecisionData decision) {
	}

	record HistoryData(WorkOrderStatus from, WorkOrderStatus to, Instant at, String note, ActorData by) {
	}

	static WorkOrderResponse forStaff(WorkOrder wo) {
		return from(wo, true);
	}

	static WorkOrderResponse forCustomer(WorkOrder wo) {
		return from(wo, false);
	}

	private static WorkOrderResponse from(WorkOrder wo, boolean includeActors) {
		return new WorkOrderResponse(wo.id(), wo.number(), wo.status(),
				new CustomerData(wo.customer().id(), wo.customer().name(), wo.customer().document(), wo.customer().email()),
				new VehicleData(wo.vehicle().id(), wo.vehicle().plate(), wo.vehicle().brand(), wo.vehicle().model(), wo.vehicle().year()),
				wo.complaint(),
				wo.draftItems().stream().map(i -> new ItemData(i.id(), i.type(), i.referenceId(), i.name(), i.unitPrice().amount(),
						i.quantity(), i.subtotal().amount())).toList(),
				wo.budgets().stream().map(b -> budget(b, includeActors)).toList(), wo.approvedTotal().amount(),
				wo.history().stream().map(h -> new HistoryData(h.from(), h.to(), h.occurredAt(), h.note(),
						includeActors ? actor(h.actor()) : null)).toList(),
				wo.createdAt(), wo.updatedAt());
	}

	private static BudgetData budget(Budget b, boolean includeActors) {
		Decision d = b.decision();
		return new BudgetData(b.id(), b.sequence(), b.kind(), b.status(), b.previousBudgetId(), b.total().amount(), b.issuedAt(),
				b.lines().stream().map(l -> new ItemData(l.id(), l.type(), l.referenceId(), l.name(), l.unitPrice().amount(),
						l.quantity(), l.subtotal().amount())).toList(),
				d == null ? null : new DecisionData(d.channel(), d.reason(), d.at(), includeActors ? actor(d.actor()) : null));
	}

	private static ActorData actor(Actor actor) {
		return new ActorData(actor.subject(), actor.role());
	}
}
