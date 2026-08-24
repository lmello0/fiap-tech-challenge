package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.StockPolicyService;
import com.fiap.techchallenge.inventory.api.commands.CreateStockPolicyCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateStockPolicyCommand;
import com.fiap.techchallenge.inventory.api.queries.StockPolicyFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.StockPolicyInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** Full endpoint documentation lives on {@link StockPolicyControllerSwaggerDoc}. */
@RestController
@RequiredArgsConstructor
public class StockPolicyController implements StockPolicyControllerSwaggerDoc {

    private final StockPolicyService stockPolicyService;

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<StockPolicyInfo>> getAll(Pageable pageable, StockPolicyFilterQuery filter) {
        Page<StockPolicyInfo> page = stockPolicyService.listStockPolicys(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<StockPolicyInfo> getById(UUID id) {
        return ResponseEntity.ok(stockPolicyService.getById(id));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<StockPolicyInfo> create(CreateStockPolicyCommand command) {
        StockPolicyInfo rule = stockPolicyService.create(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(rule.id())
                .toUri();

        return ResponseEntity.created(location).body(rule);
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<StockPolicyInfo> update(UUID id, UpdateStockPolicyCommand command) {
        return ResponseEntity.ok(stockPolicyService.update(id, command));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<Void> delete(UUID id) {
        stockPolicyService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
