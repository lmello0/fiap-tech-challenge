package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.PurchaseOrderService;
import com.fiap.techchallenge.inventory.api.commands.PlacePurchaseOrderCommand;
import com.fiap.techchallenge.inventory.api.commands.ReceivePurchaseOrderCommand;
import com.fiap.techchallenge.inventory.api.queries.PurchaseOrderFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<PurchaseOrderInfo>> getAll(
            Pageable pageable,
            PurchaseOrderFilterQuery filter
    ) {
        Page<PurchaseOrderInfo> page = purchaseOrderService.listPurchaseOrders(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PurchaseOrderInfo> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseOrderService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PurchaseOrderInfo> place(@Valid @RequestBody PlacePurchaseOrderCommand command) {
        PurchaseOrderInfo po = purchaseOrderService.place(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(po.id())
                .toUri();

        return ResponseEntity.created(location).body(po);
    }

    @PostMapping("/{id}/receipts")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PurchaseOrderInfo> receive(
            @PathVariable UUID id,
            @Valid @RequestBody ReceivePurchaseOrderCommand command
    ) {
        return ResponseEntity.ok(purchaseOrderService.receive(id, command));
    }

    @PostMapping("/{id}/cancellation")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PurchaseOrderInfo> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseOrderService.cancel(id));
    }
}
