package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.StockService;
import com.fiap.techchallenge.inventory.api.commands.AdjustStockCommand;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.api.representation.StockMovementInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("parts/{partId}/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PartInfo> adjust(
            @PathVariable UUID partId,
            @Valid @RequestBody AdjustStockCommand command
    ) {
        return ResponseEntity.ok(stockService.adjust(partId, command));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<StockMovementInfo>> getMovements(
            @PathVariable UUID partId,
            @PageableDefault Pageable pageable
    ) {
        Page<StockMovementInfo> page = stockService.getMovements(partId, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }
}
