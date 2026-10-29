package com.fiap.workshop.catalog.application.service;

import com.fiap.workshop.catalog.api.events.ServiceCreated;
import com.fiap.workshop.catalog.api.events.ServiceDeactivated;
import com.fiap.workshop.catalog.api.events.ServiceUpdated;
import com.fiap.workshop.catalog.domain.RepairService;
import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Instant;

final class CatalogEvents {

	private CatalogEvents() {
	}

	static ServiceCreated created(RepairService s, Instant now) {
		return new ServiceCreated(UuidV7.generate(), now, s.id(), s.name(), s.price().amount(), s.estimatedMinutes());
	}

	static ServiceUpdated updated(RepairService s, Instant now) {
		return new ServiceUpdated(UuidV7.generate(), now, s.id(), s.name(), s.price().amount(), s.estimatedMinutes());
	}

	static ServiceDeactivated deactivated(RepairService s, Instant now) {
		return new ServiceDeactivated(UuidV7.generate(), now, s.id());
	}
}
