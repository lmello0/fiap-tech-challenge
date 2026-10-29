package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.api.InventoryQueries;
import com.fiap.workshop.inventory.api.PartView;
import com.fiap.workshop.inventory.application.port.out.PartRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InventoryQueriesService implements InventoryQueries {

	private final PartRepository parts;

	InventoryQueriesService(PartRepository parts) {
		this.parts = parts;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<PartView> findPartById(UUID id) {
		return parts.findById(id).map(p -> new PartView(p.id(), p.sku(), p.name(), p.unitPrice().amount(), p.active()));
	}
}
