package com.fiap.techchallenge.inventory.api;

import com.fiap.techchallenge.inventory.api.commands.AdjustStockCommand;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.api.representation.StockMovementInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockService {

    /**
     * Applies a manual correction to a part's on-hand quantity and records it as an
     * {@code ADJUSTMENT} movement. Rejects a correction that would drop on-hand below zero or below
     * what is already reserved for a work order.
     */
    PartInfo adjust(UUID partId, AdjustStockCommand command);

    Page<StockMovementInfo> getMovements(UUID partId, Pageable pageable);
}
