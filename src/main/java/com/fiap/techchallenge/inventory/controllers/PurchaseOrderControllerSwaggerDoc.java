package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.commands.PlacePurchaseOrderCommand;
import com.fiap.techchallenge.inventory.api.commands.ReceivePurchaseOrderCommand;
import com.fiap.techchallenge.inventory.api.queries.PurchaseOrderFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderInfo;
import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import com.fiap.techchallenge.shared.responses.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/** Full Swagger/OpenAPI contract for {@link PurchaseOrderController} — placing, receiving, and
 * cancelling purchase orders sent to vendors. */
@Tag(name = "Purchase Orders", description = "Placing, receiving, and cancelling purchase orders sent to vendors.")
@RequestMapping("purchase-orders")
public interface PurchaseOrderControllerSwaggerDoc {

    @Operation(
            summary = "List purchase orders",
            description = "Returns a paginated, filterable list of purchase orders. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @GetMapping
    ResponseEntity<PageResponse<PurchaseOrderInfo>> getAll(Pageable pageable, PurchaseOrderFilterQuery filter);

    @Operation(
            summary = "Get a purchase order by id",
            description = "Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No purchase order exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<PurchaseOrderInfo> getById(@Parameter(description = "Purchase order id") @PathVariable UUID id);

    @Operation(
            summary = "Place a purchase order",
            description = "Places a new purchase order with a vendor for the given part lines, and forwards it to the "
                    + "vendor's ordering system. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "The referenced vendor or a referenced part does not exist.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<PurchaseOrderInfo> place(@Valid @RequestBody PlacePurchaseOrderCommand command);

    @Operation(
            summary = "Record a receipt against a purchase order",
            description = "Records parts arriving against one or more lines, updating on-hand quantity and moving-average "
                    + "cost, and moves the order to PARTIALLY_RECEIVED or RECEIVED depending on whether every line is now "
                    + "fully received. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No purchase order exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The purchase order is not in a state that allows receiving (e.g. already RECEIVED or CANCELLED).",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/receipts")
    ResponseEntity<PurchaseOrderInfo> receive(
            @Parameter(description = "Purchase order id") @PathVariable UUID id,
            @Valid @RequestBody ReceivePurchaseOrderCommand command
    );

    @Operation(
            summary = "Cancel a purchase order",
            description = "Cancels a purchase order that has not yet been fully received. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No purchase order exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The purchase order is not in a state that allows cancelling (e.g. already RECEIVED or CANCELLED).",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/cancellation")
    ResponseEntity<PurchaseOrderInfo> cancel(@Parameter(description = "Purchase order id") @PathVariable UUID id);
}
