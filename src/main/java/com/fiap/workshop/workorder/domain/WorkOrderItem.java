package com.fiap.workshop.workorder.domain;

import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.shared.domain.UuidV7;
import java.util.UUID;

public class WorkOrderItem {

	public static final int MAX_QUANTITY = 999;

	private final UUID id;
	private final ItemType type;
	private final UUID referenceId;
	private String name;
	private Money unitPrice;
	private int quantity;

	private WorkOrderItem(UUID id, ItemType type, UUID referenceId, String name, Money unitPrice, int quantity) {
		this.id = id;
		this.type = type;
		this.referenceId = referenceId;
		this.name = name;
		this.unitPrice = unitPrice;
		this.quantity = requireQuantity(quantity);
	}

	static WorkOrderItem create(ItemType type, UUID referenceId, String name, Money unitPrice, int quantity) {
		return new WorkOrderItem(UuidV7.generate(), type, referenceId, name, unitPrice, quantity);
	}

	public static WorkOrderItem restore(UUID id, ItemType type, UUID referenceId, String name, Money unitPrice, int quantity) {
		return new WorkOrderItem(id, type, referenceId, name, unitPrice, quantity);
	}

	void increaseBy(int extra) {
		quantity = requireQuantity(quantity + extra);
	}

	void changeQuantity(int newQuantity) {
		quantity = requireQuantity(newQuantity);
	}

	boolean refresh(String newName, Money newPrice) {
		boolean changed = !name.equals(newName) || !unitPrice.equals(newPrice);
		name = newName;
		unitPrice = newPrice;
		return changed;
	}

	boolean refersTo(ItemType otherType, UUID otherReference) {
		return type == otherType && referenceId.equals(otherReference);
	}

	BudgetLine toLine() {
		return new BudgetLine(UuidV7.generate(), type, referenceId, name, unitPrice, quantity);
	}

	private static int requireQuantity(int value) {
		if (value < 1 || value > MAX_QUANTITY) {
			throw new DomainException("Quantity must be between 1 and " + MAX_QUANTITY);
		}
		return value;
	}

	public Money subtotal() {
		return unitPrice.times(quantity);
	}

	public UUID id() {
		return id;
	}

	public ItemType type() {
		return type;
	}

	public UUID referenceId() {
		return referenceId;
	}

	public String name() {
		return name;
	}

	public Money unitPrice() {
		return unitPrice;
	}

	public int quantity() {
		return quantity;
	}
}
