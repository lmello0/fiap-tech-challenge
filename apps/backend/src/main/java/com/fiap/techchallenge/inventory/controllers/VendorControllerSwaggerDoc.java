package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.commands.CreateVendorCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateVendorCommand;
import com.fiap.techchallenge.inventory.api.queries.VendorFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.VendorInfo;
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

/** Full Swagger/OpenAPI contract for {@link VendorController} — parts/services vendors used in purchase orders. */
@Tag(name = "Vendors", description = "Vendors that parts and services can be purchased from.")
@RequestMapping("vendors")
public interface VendorControllerSwaggerDoc {

    @Operation(
            summary = "List vendors",
            description = "Returns a paginated, filterable list of vendors. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @GetMapping
    ResponseEntity<PageResponse<VendorInfo>> getAll(@PageableDefault Pageable pageable, VendorFilterQuery filter);

    @Operation(
            summary = "Get a vendor by id",
            description = "Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No vendor exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<VendorInfo> getById(@Parameter(description = "Vendor id") @PathVariable UUID id);

    @Operation(
            summary = "Create a vendor",
            description = "Registers a new vendor. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @PostMapping
    ResponseEntity<VendorInfo> create(@Valid @RequestBody CreateVendorCommand command);

    @Operation(
            summary = "Update a vendor",
            description = "Updates a vendor's name and contact email. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No vendor exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}")
    ResponseEntity<VendorInfo> update(
            @Parameter(description = "Vendor id") @PathVariable UUID id,
            @Valid @RequestBody UpdateVendorCommand command
    );

    @Operation(
            summary = "Deactivate a vendor",
            description = "Deactivates a vendor so it can no longer be used on new purchase orders. "
                    + "Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "204", description = "The vendor was deactivated.")
    @ApiResponse(responseCode = "404", description = "No vendor exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deactivate(@Parameter(description = "Vendor id") @PathVariable UUID id);
}
