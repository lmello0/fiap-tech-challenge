package com.fiap.workshop.inventory.application.port.in;

import com.fiap.workshop.inventory.domain.StockMovement;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import java.util.UUID;

public interface ListStockMovementsUseCase {

	PageResult<StockMovement> execute(UUID partId, PageQuery page);
}
