package com.fiap.techchallenge.vehicle.controllers;

import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.vehicle.api.commands.CreateVehicleCommand;
import com.fiap.techchallenge.vehicle.api.commands.UpdateVehicleCommand;
import com.fiap.techchallenge.vehicle.api.queries.VehicleFilterQuery;
import com.fiap.techchallenge.vehicle.api.representation.VehicleInfo;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/**
 * Full Swagger/OpenAPI contract for {@link VehicleController}. A CUSTOMER may only list, read, create,
 * update, or deactivate their own vehicles (checked in code, not via {@code @PreAuthorize}); a
 * mismatch returns 404, not 403, so as not to reveal that another customer's vehicle exists — same
 * masking as the workorder and appointment modules. ATTENDANT and MANAGER see and manage every
 * vehicle. MECHANIC and STOCKIST have no access to this resource at all.
 */
@Tag(name = "Vehicles", description = "Customer vehicle registration and management.")
@RequestMapping("vehicles")
public interface VehicleControllerSwaggerDoc {

    @Operation(
            summary = "List vehicles",
            description = "Returns a paginated, filterable list of vehicles. Requires the CUSTOMER, ATTENDANT, or "
                    + "MANAGER role. A CUSTOMER only ever sees their own vehicles, regardless of the filter supplied; "
                    + "ATTENDANT/MANAGER see every vehicle and may filter by any customer."
    )
    @CommonApiResponses
    @GetMapping
    ResponseEntity<PageResponse<VehicleInfo>> getAllVehicles(
            VehicleFilterQuery filter,
            Pageable pageable,
            Authentication authentication
    );

    @Operation(
            summary = "Get a vehicle by id",
            description = "Requires the CUSTOMER, ATTENDANT, or MANAGER role. A CUSTOMER may only fetch their own "
                    + "vehicle; requesting another customer's vehicle returns 404, so as not to reveal it exists."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No vehicle exists with the given id, or it does not belong to the caller.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<VehicleInfo> getById(@Parameter(description = "Vehicle id") @PathVariable UUID id, Authentication authentication);

    @Operation(
            summary = "Register a vehicle",
            description = "Creates a new vehicle. Requires the CUSTOMER, ATTENDANT, or MANAGER role. A CUSTOMER "
                    + "always registers the vehicle under their own id, regardless of any customerId supplied in the "
                    + "body; ATTENDANT/MANAGER must supply the owning customerId explicitly."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "409", description = "The license plate is already registered to another vehicle.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<VehicleInfo> create(@Valid @RequestBody CreateVehicleCommand command, Authentication authentication);

    @Operation(
            summary = "Update a vehicle",
            description = "Requires the CUSTOMER, ATTENDANT, or MANAGER role. A CUSTOMER may only update their own "
                    + "vehicle; attempting to update another customer's vehicle returns 404, so as not to reveal it exists."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No vehicle exists with the given id, or it does not belong to the caller.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The license plate is already registered to another vehicle.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}")
    ResponseEntity<VehicleInfo> update(
            @Parameter(description = "Vehicle id") @PathVariable UUID id,
            @Valid @RequestBody UpdateVehicleCommand command,
            Authentication authentication
    );

    @Operation(
            summary = "Deactivate a vehicle",
            description = "Soft-deletes the vehicle. Requires the CUSTOMER, ATTENDANT, or MANAGER role. A CUSTOMER "
                    + "may only deactivate their own vehicle; attempting to deactivate another customer's vehicle "
                    + "returns 404, so as not to reveal it exists."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "204", description = "The vehicle was deactivated.")
    @ApiResponse(responseCode = "404", description = "No vehicle exists with the given id, or it does not belong to the caller.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deactivate(@Parameter(description = "Vehicle id") @PathVariable UUID id, Authentication authentication);
}
