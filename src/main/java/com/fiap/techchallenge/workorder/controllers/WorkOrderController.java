package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.queries.WorkOrderFilterQuery;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService service;

    @GetMapping
    public ResponseEntity<Page<WorkOrderInfo>> getAll(
            @PageableDefault Pageable pageable,
            @Valid WorkOrderFilterQuery filter
    ) {
        Page<WorkOrderInfo> page = service
                .getAllWorkOrders(filter, pageable);

        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderInfo> getById(@PathVariable UUID id) {
        WorkOrderInfo wo = service.getWorkOrderById(id);

        return ResponseEntity.ok(wo);
    }
}
