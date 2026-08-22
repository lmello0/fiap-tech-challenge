package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.commands.CreateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.queries.RepairServiceFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
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

/** Full Swagger/OpenAPI contract for {@link RepairServiceController} — the labor/service catalog. */
@Tag(name = "Repair Services", description = "Labor/service catalog: code, price, and estimated/average execution time.")
@RequestMapping("services")
public interface RepairServiceControllerSwaggerDoc {

    @Operation(
            summary = "List repair services",
            description = "Returns a paginated, filterable list of repair services. Requires the MECHANIC, STOCKIST, or MANAGER role."
    )
    @CommonApiResponses
    @GetMapping
    ResponseEntity<PageResponse<RepairServiceInfo>> getAll(@PageableDefault Pageable pageable, RepairServiceFilterQuery filter);

    @Operation(
            summary = "Get a repair service by id",
            description = "Requires the MECHANIC, STOCKIST, or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No repair service exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<RepairServiceInfo> getById(@Parameter(description = "Repair service id") @PathVariable UUID id);

    @Operation(
            summary = "Create a repair service",
            description = "Adds a new labor/service entry to the catalog. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "409", description = "A repair service with the given code already exists.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<RepairServiceInfo> create(@Valid @RequestBody CreateRepairServiceCommand command);

    @Operation(
            summary = "Update a repair service",
            description = "Updates a repair service's name, description, price, and estimated duration. The code "
                    + "cannot be changed. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No repair service exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}")
    ResponseEntity<RepairServiceInfo> update(
            @Parameter(description = "Repair service id") @PathVariable UUID id,
            @Valid @RequestBody UpdateRepairServiceCommand command
    );

    @Operation(
            summary = "Deactivate a repair service",
            description = "Deactivates a repair service so it can no longer be used in new Budget lines. "
                    + "Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "204", description = "The repair service was deactivated.")
    @ApiResponse(responseCode = "404", description = "No repair service exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deactivate(@Parameter(description = "Repair service id") @PathVariable UUID id);
}
