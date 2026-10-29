package com.fiap.workshop.inventory.adapter.out.persistence;

import com.fiap.workshop.inventory.application.port.out.StockReservationRepository;
import com.fiap.workshop.inventory.domain.ReservationLine;
import com.fiap.workshop.inventory.domain.StockReservation;
import com.fiap.workshop.shared.domain.UuidV7;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class StockReservationRepositoryAdapter implements StockReservationRepository {

	private final StockReservationJpaRepository jpa;

	StockReservationRepositoryAdapter(StockReservationJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public StockReservation save(StockReservation reservation) {
		StockReservationEntity entity = jpa.findById(reservation.id()).orElseGet(StockReservationEntity::new);
		entity.setId(reservation.id());
		entity.setWorkOrderId(reservation.workOrderId());
		entity.setWorkOrderNumber(reservation.workOrderNumber());
		entity.setBudgetId(reservation.budgetId());
		entity.setStatus(reservation.status());
		entity.setCreatedAt(reservation.createdAt());
		entity.setUpdatedAt(reservation.updatedAt());
		if (entity.getLines().isEmpty()) {
			for (ReservationLine line : reservation.lines()) {
				StockReservationLineEntity lineEntity = new StockReservationLineEntity();
				lineEntity.setId(UuidV7.generate());
				lineEntity.setReservation(entity);
				lineEntity.setPartId(line.partId());
				lineEntity.setQuantity(line.quantity());
				entity.getLines().add(lineEntity);
			}
		}
		return toDomain(jpa.saveAndFlush(entity));
	}

	@Override
	public Optional<StockReservation> findByBudgetId(UUID budgetId) {
		return jpa.findByBudgetId(budgetId).map(StockReservationRepositoryAdapter::toDomain);
	}

	@Override
	public List<StockReservation> findByWorkOrderId(UUID workOrderId) {
		return jpa.findByWorkOrderId(workOrderId).stream().map(StockReservationRepositoryAdapter::toDomain).toList();
	}

	@Override
	public List<StockReservation> findWaitingForParts(Collection<UUID> partIds) {
		if (partIds.isEmpty()) {
			return List.of();
		}
		return jpa.findWaitingForParts(partIds).stream().map(StockReservationRepositoryAdapter::toDomain).toList();
	}

	private static StockReservation toDomain(StockReservationEntity e) {
		List<ReservationLine> lines = e.getLines().stream().map(l -> new ReservationLine(l.getPartId(), l.getQuantity())).toList();
		return StockReservation.restore(e.getId(), e.getWorkOrderId(), e.getWorkOrderNumber(), e.getBudgetId(), lines, e.getStatus(),
				e.getCreatedAt(), e.getUpdatedAt());
	}
}
