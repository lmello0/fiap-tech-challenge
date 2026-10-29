package com.fiap.workshop.workorder.domain;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorkOrder {

	private final UUID id;
	private final String number;
	private final String complaint;
	private final Instant createdAt;
	private final List<WorkOrderItem> draftItems;
	private final List<Budget> budgets;
	private final List<StatusChange> history;
	private WorkOrderStatus status;
	private CustomerSnapshot customer;
	private VehicleSnapshot vehicle;
	private Instant updatedAt;

	private WorkOrder(UUID id, String number, WorkOrderStatus status, CustomerSnapshot customer, VehicleSnapshot vehicle,
			String complaint, List<WorkOrderItem> draftItems, List<Budget> budgets, List<StatusChange> history, Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.number = number;
		this.status = status;
		this.customer = customer;
		this.vehicle = vehicle;
		this.complaint = complaint == null || complaint.isBlank() ? null : complaint.trim();
		this.draftItems = new ArrayList<>(draftItems);
		this.budgets = new ArrayList<>(budgets);
		this.history = new ArrayList<>(history);
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static WorkOrder open(String number, CustomerSnapshot customer, VehicleSnapshot vehicle, String complaint,
			List<NewItem> items, Actor actor, Instant now) {
		WorkOrder workOrder = new WorkOrder(UuidV7.generate(), number, WorkOrderStatus.RECEIVED, customer, vehicle, complaint,
				List.of(), List.of(), List.of(), now, now);
		items.forEach(item -> workOrder.addItem(item.type(), item.referenceId(), item.name(), item.unitPrice(), item.quantity()));
		workOrder.history.add(StatusChange.of(null, WorkOrderStatus.RECEIVED, actor, "Work order opened", now));
		return workOrder;
	}

	public static WorkOrder restore(UUID id, String number, WorkOrderStatus status, CustomerSnapshot customer,
			VehicleSnapshot vehicle, String complaint, List<WorkOrderItem> draftItems, List<Budget> budgets,
			List<StatusChange> history, Instant createdAt, Instant updatedAt) {
		return new WorkOrder(id, number, status, customer, vehicle, complaint, draftItems, budgets, history, createdAt, updatedAt);
	}

	public WorkOrderItem addItem(ItemType type, UUID referenceId, String name, Money unitPrice, int quantity) {
		requireEditableItems();
		for (WorkOrderItem existing : draftItems) {
			if (existing.refersTo(type, referenceId)) {
				existing.increaseBy(quantity);
				return existing;
			}
		}
		WorkOrderItem item = WorkOrderItem.create(type, referenceId, name, unitPrice, quantity);
		draftItems.add(item);
		return item;
	}

	public void changeItemQuantity(UUID itemId, int quantity) {
		requireEditableItems();
		draftItem(itemId).changeQuantity(quantity);
	}

	public void removeItem(UUID itemId) {
		requireEditableItems();
		draftItems.remove(draftItem(itemId));
	}

	public void startDiagnosis(Actor actor, Instant now) {
		if (status != WorkOrderStatus.RECEIVED) {
			throw new WorkOrderStateException("The diagnosis can only be started while the work order is " + WorkOrderStatus.RECEIVED);
		}
		transition(WorkOrderStatus.IN_DIAGNOSIS, actor, "Diagnosis started", now);
	}

	public Budget completeDiagnosis(Actor actor, Instant now) {
		if (status != WorkOrderStatus.IN_DIAGNOSIS) {
			throw new WorkOrderStateException("The diagnosis can only be completed while the work order is " + WorkOrderStatus.IN_DIAGNOSIS);
		}
		if (draftItems.isEmpty()) {
			throw new DomainException("Add at least one item before completing the diagnosis");
		}
		Budget budget = Budget.issue(nextSequence(), BudgetKind.INITIAL, lastBudgetId(),
				draftItems.stream().map(WorkOrderItem::toLine).toList(), now);
		budgets.add(budget);
		draftItems.clear();
		transition(WorkOrderStatus.WAITING_APPROVAL, actor, "Budget " + budget.sequence() + " issued", now);
		return budget;
	}

	public Budget proposeAdditionalBudget(List<NewItem> items, Actor actor, Instant now) {
		if (status != WorkOrderStatus.IN_EXECUTION) {
			throw new WorkOrderStateException("An additional budget can only be proposed while the work order is " + WorkOrderStatus.IN_EXECUTION);
		}
		if (items.isEmpty()) {
			throw new DomainException("An additional budget needs at least one item");
		}
		Map<String, WorkOrderItem> merged = new LinkedHashMap<>();
		for (NewItem item : items) {
			String key = item.type() + ":" + item.referenceId();
			WorkOrderItem existing = merged.get(key);
			if (existing == null) {
				merged.put(key, WorkOrderItem.create(item.type(), item.referenceId(), item.name(), item.unitPrice(), item.quantity()));
			} else {
				existing.increaseBy(item.quantity());
			}
		}
		Budget budget = Budget.issue(nextSequence(), BudgetKind.ADDITIONAL, lastBudgetId(),
				merged.values().stream().map(WorkOrderItem::toLine).toList(), now);
		budgets.add(budget);
		transition(WorkOrderStatus.WAITING_APPROVAL, actor, "Additional budget " + budget.sequence() + " issued", now);
		return budget;
	}

	public Budget approveBudget(UUID budgetId, Decision decision, Instant now) {
		requireWaitingApproval("approved");
		Budget budget = pendingBudget(budgetId);
		budget.approve(decision);
		if (budget.hasParts()) {
			updatedAt = now;
		} else {
			transition(WorkOrderStatus.IN_EXECUTION, decision.actor(), "Budget " + budget.sequence() + " approved", now);
		}
		return budget;
	}

	public Budget rejectBudget(UUID budgetId, Decision decision, Instant now) {
		requireWaitingApproval("rejected");
		Budget budget = pendingBudget(budgetId);
		budget.reject(decision);
		if (budget.kind() == BudgetKind.INITIAL) {
			budget.lines().forEach(line -> draftItems.add(
					WorkOrderItem.restore(UuidV7.generate(), line.type(), line.referenceId(), line.name(), line.unitPrice(), line.quantity())));
			transition(WorkOrderStatus.IN_DIAGNOSIS, decision.actor(), "Budget " + budget.sequence() + " rejected, back to diagnosis", now);
		} else {
			transition(WorkOrderStatus.IN_EXECUTION, decision.actor(), "Additional budget " + budget.sequence() + " rejected", now);
		}
		return budget;
	}

	public boolean isLastBudget(UUID budgetId) {
		return !budgets.isEmpty() && budgets.getLast().id().equals(budgetId);
	}

	public boolean awaitingStock() {
		return status == WorkOrderStatus.WAITING_APPROVAL && !budgets.isEmpty()
				&& budgets.getLast().status() == BudgetStatus.APPROVED;
	}

	public void startExecution(Actor actor, Instant now) {
		if (!awaitingStock() && status != WorkOrderStatus.WAITING_PARTS) {
			throw new WorkOrderStateException("The work order is not waiting for parts to start execution");
		}
		transition(WorkOrderStatus.IN_EXECUTION, actor, "Parts reserved", now);
	}

	public void waitForParts(Actor actor, Instant now) {
		if (!awaitingStock()) {
			throw new WorkOrderStateException("The work order has no approved budget waiting for stock");
		}
		transition(WorkOrderStatus.WAITING_PARTS, actor, "Parts shortage", now);
	}

	public void complete(Actor actor, Instant now) {
		transition(WorkOrderStatus.COMPLETED, actor, "Service completed", now);
	}

	public void deliver(Actor actor, Instant now) {
		transition(WorkOrderStatus.DELIVERED, actor, "Vehicle delivered", now);
	}

	public void cancel(String reason, Actor actor, Instant now) {
		if (!status.canTransitionTo(WorkOrderStatus.CANCELLED)) {
			throw new WorkOrderStateException("A work order in status " + status + " cannot be cancelled");
		}
		budgets.forEach(Budget::cancel);
		transition(WorkOrderStatus.CANCELLED, actor, reason == null || reason.isBlank() ? "Cancelled" : reason.trim(), now);
	}

	public boolean refreshCustomer(CustomerSnapshot snapshot) {
		if (status.terminal() || customer.equals(snapshot)) {
			return false;
		}
		customer = snapshot;
		return true;
	}

	public boolean refreshVehicle(VehicleSnapshot snapshot) {
		if (status.terminal() || vehicle.equals(snapshot)) {
			return false;
		}
		vehicle = snapshot;
		return true;
	}

	public boolean refreshDraftItem(ItemType type, UUID referenceId, String name, Money unitPrice) {
		boolean changed = false;
		for (WorkOrderItem item : draftItems) {
			if (item.refersTo(type, referenceId)) {
				changed |= item.refresh(name, unitPrice);
			}
		}
		return changed;
	}

	public List<PartQuantity> approvedParts() {
		Map<UUID, Integer> byPart = new LinkedHashMap<>();
		budgets.stream().filter(b -> b.status() == BudgetStatus.APPROVED)
				.flatMap(b -> b.parts().stream())
				.forEach(p -> byPart.merge(p.partId(), p.quantity(), Integer::sum));
		return byPart.entrySet().stream().map(e -> new PartQuantity(e.getKey(), e.getValue())).toList();
	}

	public Money approvedTotal() {
		return budgets.stream().filter(b -> b.status() == BudgetStatus.APPROVED).map(Budget::total).reduce(Money.ZERO, Money::plus);
	}

	public Budget budget(UUID budgetId) {
		return budgets.stream().filter(b -> b.id().equals(budgetId)).findFirst()
				.orElseThrow(() -> new com.fiap.workshop.shared.domain.NotFoundException("Budget " + budgetId + " not found"));
	}

	private void transition(WorkOrderStatus target, Actor actor, String note, Instant now) {
		if (!status.canTransitionTo(target)) {
			throw new WorkOrderStateException("Cannot move a work order from " + status + " to " + target);
		}
		history.add(StatusChange.of(status, target, actor, note, now));
		status = target;
		updatedAt = now;
	}

	private void requireEditableItems() {
		if (!status.itemsEditable()) {
			throw new WorkOrderStateException("Items can only be changed while the work order is " + WorkOrderStatus.RECEIVED
					+ " or " + WorkOrderStatus.IN_DIAGNOSIS);
		}
	}

	private void requireWaitingApproval(String action) {
		if (status != WorkOrderStatus.WAITING_APPROVAL) {
			throw new WorkOrderStateException("A budget can only be " + action + " while the work order is " + WorkOrderStatus.WAITING_APPROVAL);
		}
	}

	private WorkOrderItem draftItem(UUID itemId) {
		return draftItems.stream().filter(i -> i.id().equals(itemId)).findFirst()
				.orElseThrow(() -> new com.fiap.workshop.shared.domain.NotFoundException("Item " + itemId + " not found"));
	}

	private Budget pendingBudget(UUID budgetId) {
		Budget budget = budget(budgetId);
		if (budget.status() != BudgetStatus.PENDING) {
			throw new WorkOrderStateException("Budget " + budgetId + " was already " + budget.status());
		}
		return budget;
	}

	private int nextSequence() {
		return budgets.size() + 1;
	}

	private UUID lastBudgetId() {
		return budgets.isEmpty() ? null : budgets.getLast().id();
	}

	public UUID id() {
		return id;
	}

	public String number() {
		return number;
	}

	public WorkOrderStatus status() {
		return status;
	}

	public CustomerSnapshot customer() {
		return customer;
	}

	public VehicleSnapshot vehicle() {
		return vehicle;
	}

	public String complaint() {
		return complaint;
	}

	public List<WorkOrderItem> draftItems() {
		return Collections.unmodifiableList(draftItems);
	}

	public List<Budget> budgets() {
		return Collections.unmodifiableList(budgets);
	}

	public List<StatusChange> history() {
		return Collections.unmodifiableList(history);
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}
}
