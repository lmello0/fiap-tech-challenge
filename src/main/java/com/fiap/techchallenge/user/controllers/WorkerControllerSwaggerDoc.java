package com.fiap.techchallenge.user.controllers;

import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.user.api.commands.UpdateUserProfileCommand;
import com.fiap.techchallenge.user.api.queries.UserFilterQuery;
import com.fiap.techchallenge.user.api.representation.UserInfo;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/**
 * Full Swagger/OpenAPI contract for {@link WorkerController} — MANAGER-only worker management,
 * plus a worker's self-service profile update.
 */
@Tag(name = "Workers", description = "MANAGER-only worker management, plus a worker's self-service profile update.")
@RequestMapping("workers")
public interface WorkerControllerSwaggerDoc {

    @Operation(
            summary = "List workers",
            description = "Returns a paginated, filterable list of worker accounts. Requires the MANAGER role."
    )
    @CommonApiResponses
    @GetMapping
    ResponseEntity<PageResponse<UserInfo>> list(UserFilterQuery filter, Pageable pageable);

    @Operation(
            summary = "Get a worker by id",
            description = "Requires the MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No user exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<UserInfo> getById(@Parameter(description = "Worker id") @PathVariable UUID id);

    @Operation(
            summary = "Update a worker's profile",
            description = "Updates the given worker's name and phone numbers. Only the worker themselves may call "
                    + "this — unlike customer profile updates, there is no MANAGER override."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No user exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}")
    ResponseEntity<UserInfo> update(
            @Parameter(description = "Worker id") @PathVariable UUID id,
            @Valid @RequestBody UpdateUserProfileCommand command
    );

    @Operation(
            summary = "Terminate a worker",
            description = "Ends the given user's Worker facet as of today. Requires the MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "204", description = "The worker was terminated.")
    @ApiResponse(responseCode = "404", description = "No user exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "The user does not have a Worker facet.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> terminate(@Parameter(description = "Worker id") @PathVariable UUID id);
}
