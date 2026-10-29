package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.in.ListStockMovementsUseCase;
import com.fiap.workshop.inventory.application.port.out.PartRepository;
import com.fiap.workshop.inventory.application.port.out.StockMovementRepository;
import com.fiap.workshop.inventory.domain.StockMovement;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListStockMovementsService implements ListStockMovementsUseCase {

	private final PartRepository parts;
	private final StockMovementRepository movements;

	ListStockMovementsService(PartRepository parts, StockMovementRepository movements) {
		this.parts = parts;
		this.movements = movements;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<StockMovement> execute(UUID partId, PageQuery page) {
		parts.findById(partId).orElseThrow(() -> new NotFoundException("Part " + partId + " not found"));
		return movements.findByPart(partId, page);
	}
}
