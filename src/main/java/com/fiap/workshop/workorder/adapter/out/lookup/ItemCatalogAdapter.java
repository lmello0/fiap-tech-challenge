package com.fiap.workshop.workorder.adapter.out.lookup;

import com.fiap.workshop.catalog.api.CatalogQueries;
import com.fiap.workshop.inventory.api.InventoryQueries;
import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.workorder.application.port.out.ItemCatalog;
import com.fiap.workshop.workorder.domain.ItemType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ItemCatalogAdapter implements ItemCatalog {

	private final CatalogQueries services;
	private final InventoryQueries parts;

	ItemCatalogAdapter(CatalogQueries services, InventoryQueries parts) {
		this.services = services;
		this.parts = parts;
	}

	@Override
	public Optional<ItemInfo> find(ItemType type, UUID id) {
		return switch (type) {
			case SERVICE -> services.findServiceById(id)
					.map(s -> new ItemInfo(type, s.id(), s.name(), new Money(s.price()), s.active()));
			case PART -> parts.findPartById(id)
					.map(p -> new ItemInfo(type, p.id(), p.name(), new Money(p.unitPrice()), p.active()));
		};
	}
}
