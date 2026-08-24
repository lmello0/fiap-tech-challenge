package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.commands.CreateStockPolicyCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateStockPolicyCommand;
import com.fiap.techchallenge.inventory.api.queries.StockPolicyFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.StockPolicyInfo;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/** Full Swagger/OpenAPI contract for {@link StockPolicyController} — per-part, per-vendor
 * automatic-reorder thresholds. */
@Tag(name = "Stock Policies", description = "Per-part, per-vendor automatic-reorder thresholds.")
@RequestMapping("stock-policies")
public interface StockPolicyControllerSwaggerDoc {

    @Operation(
            summary = "List stock policys",
            description = "Returns a paginated, filterable list of stock policys. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @GetMapping
    ResponseEntity<PageResponse<StockPolicyInfo>> getAll(
            @PageableDefault Pageable pageable,
            StockPolicyFilterQuery filter
    );

    @Operation(
            summary = "Get a stock policy by id",
            description = "Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No stock policy exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<StockPolicyInfo> getById(@Parameter(description = "Stock policy id") @PathVariable UUID id);

    @Operation(
            summary = "Create a stock policy",
            description = "Creates a minimum/maximum quantity threshold for a part, pointing at the vendor to reorder "
                    + "from. Evaluated immediately, so a part already below the given minimum triggers a reorder "
                    + "signal right away. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "The referenced part or vendor does not exist.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<StockPolicyInfo> create(@Valid @RequestBody CreateStockPolicyCommand command);

    @Operation(
            summary = "Update a stock policy",
            description = "Updates a stock policy's thresholds, vendor, and enabled flag, and re-evaluates the part "
                    + "immediately. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No stock policy exists with the given id, or the referenced vendor does not exist.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}")
    ResponseEntity<StockPolicyInfo> update(
            @Parameter(description = "Stock policy id") @PathVariable UUID id,
            @Valid @RequestBody UpdateStockPolicyCommand command
    );

    @Operation(
            summary = "Delete a stock policy",
            description = "Permanently deletes a stock policy. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "204", description = "The stock policy was deleted.")
    @ApiResponse(responseCode = "404", description = "No stock policy exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@Parameter(description = "Stock policy id") @PathVariable UUID id);
}
