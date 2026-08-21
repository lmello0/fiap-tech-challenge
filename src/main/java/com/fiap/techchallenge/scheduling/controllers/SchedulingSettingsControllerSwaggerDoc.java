package com.fiap.techchallenge.scheduling.controllers;

import com.fiap.techchallenge.scheduling.api.commands.CreateClosureCommand;
import com.fiap.techchallenge.scheduling.api.commands.UpdateSchedulingSettingsCommand;
import com.fiap.techchallenge.scheduling.api.representation.ClosureInfo;
import com.fiap.techchallenge.scheduling.api.representation.SchedulingSettingsInfo;
import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;

/** Full Swagger/OpenAPI contract for {@link SchedulingSettingsController} — shop-wide scheduling
 * settings and closure days. */
@Tag(name = "Scheduling Settings", description = "Shop-wide scheduling settings and closure days.")
@RequestMapping("scheduling")
public interface SchedulingSettingsControllerSwaggerDoc {

    @Operation(
            summary = "Get scheduling settings",
            description = "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @GetMapping("/settings")
    ResponseEntity<SchedulingSettingsInfo> getSettings();

    @Operation(
            summary = "Update scheduling settings",
            description = "Requires the MANAGER role."
    )
    @CommonApiResponses
    @PutMapping("/settings")
    ResponseEntity<SchedulingSettingsInfo> updateSettings(@Valid @RequestBody UpdateSchedulingSettingsCommand command);

    @Operation(
            summary = "List closure days",
            description = "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @GetMapping("/closures")
    ResponseEntity<List<ClosureInfo>> listClosures();

    @Operation(
            summary = "Create a closure day",
            description = "Blocks the given date off from scheduling. Requires the MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "409", description = "A closure already exists for the given date.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/closures")
    ResponseEntity<ClosureInfo> createClosure(@Valid @RequestBody CreateClosureCommand command);

    @Operation(
            summary = "Delete a closure day",
            description = "Idempotent: succeeds whether or not a closure exists on the given date. Requires the MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "204", description = "The closure was deleted, or none existed on the given date.")
    @DeleteMapping("/closures/{date}")
    ResponseEntity<Void> deleteClosure(@Parameter(description = "Closure date") @PathVariable LocalDate date);
}
