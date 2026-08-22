package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.PurchaseOrderService;
import com.fiap.techchallenge.inventory.api.commands.PlacePurchaseOrderCommand;
import com.fiap.techchallenge.inventory.api.commands.ReceivePurchaseOrderCommand;
import com.fiap.techchallenge.inventory.api.queries.PurchaseOrderFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderInfo;
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

/** Full endpoint documentation lives on {@link PurchaseOrderControllerSwaggerDoc}. */
@RestController
@RequiredArgsConstructor
public class PurchaseOrderController implements PurchaseOrderControllerSwaggerDoc {

    private final PurchaseOrderService purchaseOrderService;

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<PurchaseOrderInfo>> getAll(Pageable pageable, PurchaseOrderFilterQuery filter) {
        Page<PurchaseOrderInfo> page = purchaseOrderService.listPurchaseOrders(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PurchaseOrderInfo> getById(UUID id) {
        return ResponseEntity.ok(purchaseOrderService.getById(id));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PurchaseOrderInfo> place(PlacePurchaseOrderCommand command) {
        PurchaseOrderInfo po = purchaseOrderService.place(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(po.id())
                .toUri();

        return ResponseEntity.created(location).body(po);
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PurchaseOrderInfo> receive(UUID id, ReceivePurchaseOrderCommand command) {
        return ResponseEntity.ok(purchaseOrderService.receive(id, command));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PurchaseOrderInfo> cancel(UUID id) {
        return ResponseEntity.ok(purchaseOrderService.cancel(id));
    }
}
