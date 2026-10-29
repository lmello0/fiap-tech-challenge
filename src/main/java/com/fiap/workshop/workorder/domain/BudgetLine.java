package com.fiap.workshop.workorder.domain;

import com.fiap.workshop.shared.domain.Money;
import java.util.UUID;

public record BudgetLine(UUID id, ItemType type, UUID referenceId, String name, Money unitPrice, int quantity) {

	public Money subtotal() {
		return unitPrice.times(quantity);
	}
}
