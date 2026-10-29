package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.out.PartRepository;
import com.fiap.workshop.inventory.application.port.out.StockMovementRepository;
import com.fiap.workshop.inventory.application.port.out.StockReservationRepository;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.inventory.domain.ReservationLine;
import com.fiap.workshop.inventory.domain.ReservationStatus;
import com.fiap.workshop.inventory.domain.StockMovement;
import com.fiap.workshop.inventory.domain.StockReservation;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
		public List<Part> findAllForUpdate(Collection<UUID> ids) {
			return ids.stream().sorted().map(store::get).filter(java.util.Objects::nonNull).toList();
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

	static class Reservations implements StockReservationRepository {

		final Map<UUID, StockReservation> store = new LinkedHashMap<>();

		@Override
		public StockReservation save(StockReservation reservation) {
			store.put(reservation.id(), reservation);
			return reservation;
		}

		@Override
		public Optional<StockReservation> findByBudgetId(UUID budgetId) {
			return store.values().stream().filter(r -> r.budgetId().equals(budgetId)).findFirst();
		}

		@Override
		public List<StockReservation> findByWorkOrderId(UUID workOrderId) {
			return store.values().stream().filter(r -> r.workOrderId().equals(workOrderId)).toList();
		}

		@Override
		public List<StockReservation> findWaitingForParts(Collection<UUID> partIds) {
			return store.values().stream().filter(r -> r.status() == ReservationStatus.WAITING)
					.filter(r -> r.lines().stream().map(ReservationLine::partId).anyMatch(partIds::contains))
					.sorted(Comparator.comparing(StockReservation::createdAt)).toList();
		}
	}
}
