package com.fiap.workshop.inventory.application.port.out;

import com.fiap.workshop.inventory.domain.StockMovement;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import java.util.UUID;

public interface StockMovementRepository {

	StockMovement save(StockMovement movement);

	PageResult<StockMovement> findByPart(UUID partId, PageQuery page);
}
