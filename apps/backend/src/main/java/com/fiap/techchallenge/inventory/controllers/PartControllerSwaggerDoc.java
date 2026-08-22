package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.commands.CreatePartCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdatePartCommand;
import com.fiap.techchallenge.inventory.api.queries.PartFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/** Full Swagger/OpenAPI contract for {@link PartController} — the inventory Part catalog. */
@Tag(name = "Parts", description = "Inventory Part catalog: SKU, pricing, and on-hand/reserved stock levels.")
@RequestMapping("parts")
public interface PartControllerSwaggerDoc {

    @Operation(
            summary = "List parts",
            description = "Returns a paginated, filterable list of parts. Requires the MECHANIC, STOCKIST, or MANAGER role."
    )
    @CommonApiResponses
    @GetMapping
    ResponseEntity<PageResponse<PartInfo>> getAll(Pageable pageable, PartFilterQuery filter);

    @Operation(
            summary = "Get a part by id",
            description = "Requires the MECHANIC, STOCKIST, or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No part exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<PartInfo> getById(@Parameter(description = "Part id") @PathVariable UUID id);

    @Operation(
            summary = "Create a part",
            description = "Adds a new part to the catalog. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "409", description = "A part with the given SKU already exists.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<PartInfo> create(@Valid @RequestBody CreatePartCommand command);

    @Operation(
            summary = "Update a part",
            description = "Updates a part's catalog details (name, description, brand, unit of measure, sale price). "
                    + "The SKU cannot be changed. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No part exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}")
    ResponseEntity<PartInfo> update(
            @Parameter(description = "Part id") @PathVariable UUID id,
            @Valid @RequestBody UpdatePartCommand command
    );

    @Operation(
            summary = "Deactivate a part",
            description = "Deactivates a part so it can no longer be used in new Budget lines or purchase orders. "
                    + "Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "204", description = "The part was deactivated.")
    @ApiResponse(responseCode = "404", description = "No part exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deactivate(@Parameter(description = "Part id") @PathVariable UUID id);
}
