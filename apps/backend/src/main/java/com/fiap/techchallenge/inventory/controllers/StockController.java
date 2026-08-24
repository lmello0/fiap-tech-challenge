package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.StockService;
import com.fiap.techchallenge.inventory.api.commands.AdjustStockCommand;
import com.fiap.techchallenge.inventory.api.representation.PartStockInfo;
import com.fiap.techchallenge.inventory.api.representation.StockMovementInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Full endpoint documentation lives on {@link StockControllerSwaggerDoc}. */
@RestController
@RequiredArgsConstructor
public class StockController implements StockControllerSwaggerDoc {

    private final StockService stockService;

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PartStockInfo> adjust(UUID partId, AdjustStockCommand command) {
        return ResponseEntity.ok(stockService.adjust(partId, command));
    }

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<StockMovementInfo>> getMovements(UUID partId, Pageable pageable) {
        Page<StockMovementInfo> page = stockService.getMovements(partId, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }
}
