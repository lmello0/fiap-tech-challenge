package com.fiap.workshop.inventory.adapter.in.web;

import com.fiap.workshop.inventory.domain.MovementType;
import com.fiap.workshop.inventory.domain.StockMovement;
import java.time.Instant;
import java.util.UUID;

record StockMovementResponse(UUID id, UUID partId, MovementType type, int quantity, int balanceAfter, String reason,
		String performedBy, Instant createdAt) {

	static StockMovementResponse from(StockMovement m) {
		return new StockMovementResponse(m.id(), m.partId(), m.type(), m.quantity(), m.balanceAfter(), m.reason(), m.performedBy(),
				m.createdAt());
	}
}
