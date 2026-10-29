package com.fiap.workshop.inventory.domain;

import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Instant;
import java.util.UUID;

public record StockMovement(UUID id, UUID partId, MovementType type, int quantity, int balanceAfter, String reason,
		String performedBy, Instant createdAt) {

	public static StockMovement record(UUID partId, MovementType type, int quantity, int balanceAfter, String reason,
			String performedBy, Instant now) {
		String normalizedReason = reason == null || reason.isBlank() ? null : reason.trim();
		return new StockMovement(UuidV7.generate(), partId, type, quantity, balanceAfter, normalizedReason, performedBy, now);
	}
}
