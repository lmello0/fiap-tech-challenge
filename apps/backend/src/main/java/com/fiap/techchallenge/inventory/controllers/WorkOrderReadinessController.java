package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.PartReservationService;
import com.fiap.techchallenge.inventory.api.representation.BlockingShortfallInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Answers "is this work order blocked from starting service because of parts?" before anyone clicks
 * Start — parts only; the work-order module composes this with its own reasons (budget, assignment,
 * status).
 */
@Tag(name = "Work Order Readiness", description = "Whether a work order's parts are fully reserved yet.")
@RestController
@RequestMapping("work-orders/{workOrderId}/blocking-shortfalls")
@RequiredArgsConstructor
public class WorkOrderReadinessController {

    private final PartReservationService partReservationService;

    @Operation(
            summary = "List the parts still blocking a work order from starting service",
            description = "Empty means nothing here is blocking it. Each entry is a part still short on this work "
                    + "order's reservations. Requires the MECHANIC, STOCKIST, or MANAGER role."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<List<BlockingShortfallInfo>> getBlockingShortfalls(
            @Parameter(description = "Work order id") @PathVariable UUID workOrderId) {
        return ResponseEntity.ok(partReservationService.getBlockingShortfalls(workOrderId));
    }
}
