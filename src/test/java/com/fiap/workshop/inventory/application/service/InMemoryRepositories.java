package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.out.PartRepository;
import com.fiap.workshop.inventory.application.port.out.StockMovementRepository;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.inventory.domain.StockMovement;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class InMemoryRepositories {

	private InMemoryRepositories() {
	}

	static class Parts implements PartRepository {

		final Map<UUID, Part> store = new LinkedHashMap<>();

		@Override
		public Part save(Part part) {
			store.put(part.id(), part);
			return part;
		}

		@Override
		public Optional<Part> findById(UUID id) {
			return Optional.ofNullable(store.get(id));
		}

		@Override
		public boolean existsBySku(String sku) {
			return store.values().stream().anyMatch(p -> p.sku().equals(sku));
		}

		@Override
		public PageResult<Part> search(PartFilter filter, PageQuery page) {
			List<Part> all = store.values().stream()
					.filter(p -> filter.includeInactive() || p.active())
					.filter(p -> !filter.lowStockOnly() || p.lowStock())
					.filter(p -> filter.text() == null || p.name().toLowerCase().contains(filter.text().toLowerCase()))
					.toList();
			return new PageResult<>(all.stream().skip((long) page.page() * page.size()).limit(page.size()).toList(), page.page(),
					page.size(), all.size());
		}
	}

	static class Movements implements StockMovementRepository {

		final List<StockMovement> store = new ArrayList<>();

		@Override
		public StockMovement save(StockMovement movement) {
			store.add(movement);
			return movement;
		}

		@Override
		public PageResult<StockMovement> findByPart(UUID partId, PageQuery page) {
			List<StockMovement> all = store.stream().filter(m -> m.partId().equals(partId)).toList();
			return new PageResult<>(all.stream().skip((long) page.page() * page.size()).limit(page.size()).toList(), page.page(),
					page.size(), all.size());
		}
	}
}
