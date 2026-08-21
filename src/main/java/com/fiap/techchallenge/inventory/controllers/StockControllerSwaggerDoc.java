package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.commands.AdjustStockCommand;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.api.representation.StockMovementInfo;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/** Full Swagger/OpenAPI contract for {@link StockController} — manual stock corrections and the
 * resulting movement ledger for a single part. */
@Tag(name = "Stock", description = "Manual stock corrections and the movement ledger for a single part.")
@RequestMapping("parts/{partId}/stock")
public interface StockControllerSwaggerDoc {

    @Operation(
            summary = "Adjust a part's on-hand quantity",
            description = "Applies a manual correction to a part's on-hand quantity — a physical count, breakage, or "
                    + "stock found on the wrong shelf. The quantity is signed: positive adds stock, negative removes "
                    + "it; zero is rejected. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No part exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "The adjustment would drop on-hand quantity below zero, or below what is already reserved.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/adjustments")
    ResponseEntity<PartInfo> adjust(
            @Parameter(description = "Part id") @PathVariable UUID partId,
            @Valid @RequestBody AdjustStockCommand command
    );

    @Operation(
            summary = "Get a part's stock movement ledger",
            description = "Returns a paginated, most-recent-first list of every stock movement recorded for a part "
                    + "(purchases, adjustments, reservations, and releases). Requires the MECHANIC, STOCKIST, or "
                    + "MANAGER role."
    )
    @CommonApiResponses
    @GetMapping("/movements")
    ResponseEntity<PageResponse<StockMovementInfo>> getMovements(
            @Parameter(description = "Part id") @PathVariable UUID partId,
            @PageableDefault Pageable pageable
    );
}
