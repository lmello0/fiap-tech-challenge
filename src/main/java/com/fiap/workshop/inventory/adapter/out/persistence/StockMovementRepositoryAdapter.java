package com.fiap.workshop.inventory.adapter.out.persistence;

import com.fiap.workshop.inventory.application.port.out.StockMovementRepository;
import com.fiap.workshop.inventory.domain.StockMovement;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class StockMovementRepositoryAdapter implements StockMovementRepository {

	private final StockMovementJpaRepository jpa;

	StockMovementRepositoryAdapter(StockMovementJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public StockMovement save(StockMovement movement) {
		StockMovementEntity entity = new StockMovementEntity();
		entity.setId(movement.id());
		entity.setPartId(movement.partId());
		entity.setType(movement.type());
		entity.setQuantity(movement.quantity());
		entity.setBalanceAfter(movement.balanceAfter());
		entity.setReason(movement.reason());
		entity.setPerformedBy(movement.performedBy());
		entity.setCreatedAt(movement.createdAt());
		return toDomain(jpa.saveAndFlush(entity));
	}

	@Override
	public PageResult<StockMovement> findByPart(UUID partId, PageQuery page) {
		Page<StockMovementEntity> result = jpa.findByPartId(partId,
				PageRequest.of(page.page(), page.size(), Sort.by(Sort.Direction.DESC, "createdAt", "id")));
		return new PageResult<>(result.map(StockMovementRepositoryAdapter::toDomain).getContent(), page.page(), page.size(),
				result.getTotalElements());
	}

	private static StockMovement toDomain(StockMovementEntity e) {
		return new StockMovement(e.getId(), e.getPartId(), e.getType(), e.getQuantity(), e.getBalanceAfter(), e.getReason(),
				e.getPerformedBy(), e.getCreatedAt());
	}
}
