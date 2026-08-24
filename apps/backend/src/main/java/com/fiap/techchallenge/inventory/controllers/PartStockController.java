package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.StockService;
import com.fiap.techchallenge.inventory.api.queries.PartStockFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PartStockInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Derived stock reads: on-hand, available, {@code StockStatus}, and windowed average cost per part —
 * everything a worker needs to list/filter parts by stock without reading the ledger directly.
 */
@RestController
@RequiredArgsConstructor
public class PartStockController implements PartStockControllerSwaggerDoc {

    private final StockService stockService;

    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<PartStockInfo>> getAll(Pageable pageable, PartStockFilterQuery filter) {
        Page<PartStockInfo> page = stockService.listStock(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<PartStockInfo> getByPartId(UUID partId) {
        return ResponseEntity.ok(stockService.getStock(partId));
    }
}
