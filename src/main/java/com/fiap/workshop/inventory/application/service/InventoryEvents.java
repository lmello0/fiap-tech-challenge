package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.api.events.LowStockDetected;
import com.fiap.workshop.inventory.api.events.PartCreated;
import com.fiap.workshop.inventory.api.events.PartDeactivated;
import com.fiap.workshop.inventory.api.events.PartUpdated;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Instant;

final class InventoryEvents {

	private InventoryEvents() {
	}

	static PartCreated created(Part p, Instant now) {
		return new PartCreated(UuidV7.generate(), now, p.id(), p.sku(), p.name(), p.unitPrice().amount());
	}

	static PartUpdated updated(Part p, Instant now) {
		return new PartUpdated(UuidV7.generate(), now, p.id(), p.sku(), p.name(), p.unitPrice().amount());
	}

	static PartDeactivated deactivated(Part p, Instant now) {
		return new PartDeactivated(UuidV7.generate(), now, p.id());
	}

	static LowStockDetected lowStock(Part p, Instant now) {
		return new LowStockDetected(UuidV7.generate(), now, p.id(), p.sku(), p.name(), p.onHand(), p.minimumQuantity());
	}
}
