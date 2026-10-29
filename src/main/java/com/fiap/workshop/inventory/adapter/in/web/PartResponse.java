package com.fiap.workshop.inventory.adapter.in.web;

import com.fiap.workshop.inventory.domain.Part;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record PartResponse(UUID id, String sku, String name, String description, BigDecimal unitPrice, int minimumQuantity, int onHand,
		int reserved, int available, boolean lowStock, boolean active, Instant createdAt, Instant updatedAt) {

	static PartResponse from(Part p) {
		return new PartResponse(p.id(), p.sku(), p.name(), p.description(), p.unitPrice().amount(), p.minimumQuantity(), p.onHand(),
				p.reserved(), p.available(), p.lowStock(), p.active(), p.createdAt(), p.updatedAt());
	}
}
