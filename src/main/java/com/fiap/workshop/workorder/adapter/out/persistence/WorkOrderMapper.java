package com.fiap.workshop.workorder.adapter.out.persistence;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.workorder.domain.Budget;
import com.fiap.workshop.workorder.domain.BudgetLine;
import com.fiap.workshop.workorder.domain.CustomerSnapshot;
import com.fiap.workshop.workorder.domain.Decision;
import com.fiap.workshop.workorder.domain.StatusChange;
import com.fiap.workshop.workorder.domain.VehicleSnapshot;
import com.fiap.workshop.workorder.domain.WorkOrder;
import com.fiap.workshop.workorder.domain.WorkOrderItem;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class WorkOrderMapper {

	private WorkOrderMapper() {
	}

	static WorkOrder toDomain(WorkOrderEntity e) {
		List<WorkOrderItem> items = e.getItems().stream()
				.map(i -> WorkOrderItem.restore(i.getId(), i.getType(), i.getReferenceId(), i.getName(), new Money(i.getUnitPrice()),
						i.getQuantity()))
				.toList();
		List<Budget> budgets = e.getBudgets().stream().map(WorkOrderMapper::toDomain).toList();
		List<StatusChange> history = e.getHistory().stream()
				.map(h -> new StatusChange(h.getId(), h.getFromStatus(), h.getToStatus(), h.getOccurredAt(),
						new Actor(h.getActorSubject(), h.getActorRole()), h.getNote()))
				.toList();
		return WorkOrder.restore(e.getId(), e.getNumber(), e.getStatus(),
				new CustomerSnapshot(e.getCustomerId(), e.getCustomerName(), e.getCustomerDocument(), e.getCustomerEmail()),
				new VehicleSnapshot(e.getVehicleId(), e.getVehiclePlate(), e.getVehicleBrand(), e.getVehicleModel(), e.getVehicleYear()),
				e.getComplaint(), items, budgets, history, e.getCreatedAt(), e.getUpdatedAt());
	}

	private static Budget toDomain(BudgetEntity b) {
		List<BudgetLine> lines = b.getLines().stream()
				.map(l -> new BudgetLine(l.getId(), l.getType(), l.getReferenceId(), l.getName(), new Money(l.getUnitPrice()), l.getQuantity()))
				.toList();
		Decision decision = b.getDecidedAt() == null ? null
				: new Decision(new Actor(b.getDecidedBy(), b.getDecidedRole()), b.getDecisionChannel(), b.getDecisionReason(), b.getDecidedAt());
		return Budget.restore(b.getId(), b.getSequence(), b.getKind(), b.getPreviousBudgetId(), lines, b.getIssuedAt(), b.getStatus(), decision);
	}

	static void apply(WorkOrder wo, WorkOrderEntity e) {
		e.setId(wo.id());
		e.setNumber(wo.number());
		e.setStatus(wo.status());
		e.setCustomerId(wo.customer().id());
		e.setCustomerName(wo.customer().name());
		e.setCustomerDocument(wo.customer().document());
		e.setCustomerEmail(wo.customer().email());
		e.setVehicleId(wo.vehicle().id());
		e.setVehiclePlate(wo.vehicle().plate());
		e.setVehicleBrand(wo.vehicle().brand());
		e.setVehicleModel(wo.vehicle().model());
		e.setVehicleYear(wo.vehicle().year());
		e.setComplaint(wo.complaint());
		e.setCreatedAt(wo.createdAt());
		e.setUpdatedAt(wo.updatedAt());

		List<WorkOrderItem> items = wo.draftItems();
		sync(e.getItems(), items, WorkOrderItemEntity::getId, WorkOrderItem::id, WorkOrderItemEntity::new, (ie, item) -> {
			ie.setId(item.id());
			ie.setWorkOrder(e);
			ie.setPosition(items.indexOf(item));
			ie.setType(item.type());
			ie.setReferenceId(item.referenceId());
			ie.setName(item.name());
			ie.setUnitPrice(item.unitPrice().amount());
			ie.setQuantity(item.quantity());
		});

		sync(e.getBudgets(), wo.budgets(), BudgetEntity::getId, Budget::id, BudgetEntity::new, (be, budget) -> {
			be.setId(budget.id());
			be.setWorkOrder(e);
			be.setSequence(budget.sequence());
			be.setKind(budget.kind());
			be.setStatus(budget.status());
			be.setPreviousBudgetId(budget.previousBudgetId());
			be.setTotal(budget.total().amount());
			be.setIssuedAt(budget.issuedAt());
			Decision d = budget.decision();
			be.setDecidedAt(d == null ? null : d.at());
			be.setDecidedBy(d == null ? null : d.actor().subject());
			be.setDecidedRole(d == null ? null : d.actor().role());
			be.setDecisionChannel(d == null ? null : d.channel());
			be.setDecisionReason(d == null ? null : d.reason());
			List<BudgetLine> lines = budget.lines();
			sync(be.getLines(), lines, BudgetLineEntity::getId, BudgetLine::id, BudgetLineEntity::new, (le, line) -> {
				le.setId(line.id());
				le.setBudget(be);
				le.setPosition(lines.indexOf(line));
				le.setType(line.type());
				le.setReferenceId(line.referenceId());
				le.setName(line.name());
				le.setUnitPrice(line.unitPrice().amount());
				le.setQuantity(line.quantity());
			});
		});

		sync(e.getHistory(), wo.history(), StatusChangeEntity::getId, StatusChange::id, StatusChangeEntity::new, (he, change) -> {
			he.setId(change.id());
			he.setWorkOrder(e);
			he.setFromStatus(change.from());
			he.setToStatus(change.to());
			he.setOccurredAt(change.occurredAt());
			he.setActorSubject(change.actor().subject());
			he.setActorRole(change.actor().role());
			he.setNote(change.note());
		});
	}

	private static <E, D> void sync(List<E> entities, List<D> domain, Function<E, UUID> entityId, Function<D, UUID> domainId,
			Supplier<E> factory, BiConsumer<E, D> fill) {
		Set<UUID> keep = new HashSet<>();
		domain.forEach(d -> keep.add(domainId.apply(d)));
		entities.removeIf(entity -> !keep.contains(entityId.apply(entity)));
		for (D d : domain) {
			E existing = entities.stream().filter(entity -> entityId.apply(entity).equals(domainId.apply(d))).findFirst().orElse(null);
			if (existing == null) {
				existing = factory.get();
				entities.add(existing);
			}
			fill.accept(existing, d);
		}
	}
}
