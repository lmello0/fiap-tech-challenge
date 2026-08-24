package com.fiap.techchallenge.inventory.api;

import com.fiap.techchallenge.inventory.api.commands.AdjustStockCommand;
import com.fiap.techchallenge.inventory.api.queries.PartStockFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PartStockInfo;
import com.fiap.techchallenge.inventory.api.representation.StockMovementInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockService {

    /**
     * Applies a manual correction to a part's on-hand quantity by writing an {@code ADJUSTMENT}
     * movement. Rejects a correction that would drop derived on-hand below zero or below what is
     * already reserved for a work order. A positive correction heals open shortfalls on this part
     * FIFO, same as a purchase receipt.
     */
    PartStockInfo adjust(UUID partId, AdjustStockCommand command);

    Page<StockMovementInfo> getMovements(UUID partId, Pageable pageable);

    /** One part's derived stock standing — on-hand, available, status, and windowed cost. */
    PartStockInfo getStock(UUID partId);

    /** Filterable, e.g. by {@link com.fiap.techchallenge.inventory.enums.StockStatus} to find what's low. */
    Page<PartStockInfo> listStock(PartStockFilterQuery filter, Pageable pageable);
}
