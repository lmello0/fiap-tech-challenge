package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.queries.PartStockFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PartStockInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/**
 * Derived stock reads: on-hand, available, {@code StockStatus}, and windowed average cost per part —
 * everything a worker needs to list/filter parts by stock without reading the ledger directly.
 */
@Tag(name = "Part Stock", description = "Derived stock standing per part: on-hand, available, status, and windowed cost.")
@RequestMapping("parts/stock")
public interface PartStockControllerSwaggerDoc {

    @Operation(summary = "List parts' derived stock, filterable by status (e.g. LOW, OUT).")
    @GetMapping
    ResponseEntity<PageResponse<PartStockInfo>> getAll(@PageableDefault Pageable pageable, PartStockFilterQuery filter);

    @Operation(summary = "Get one part's derived stock standing.")
    @GetMapping("/{partId}")
    ResponseEntity<PartStockInfo> getByPartId(@Parameter(description = "Part id") @PathVariable UUID partId);
}
