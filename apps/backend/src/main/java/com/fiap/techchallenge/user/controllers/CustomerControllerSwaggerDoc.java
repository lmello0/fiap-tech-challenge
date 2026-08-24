package com.fiap.techchallenge.user.controllers;

import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/**
 * Full Swagger/OpenAPI contract for {@link CustomerController} — staff management of Customer
 * accounts, plus self-service update on one's own profile.
 */
@Tag(name = "Customers", description = "Staff management of Customer accounts: list, view, update, deactivate, reactivate.")
@RequestMapping("customers")
public interface CustomerControllerSwaggerDoc {

    @Operation(
            summary = "List customers",
            description = "Returns a paginated, filterable list of customer accounts. Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @GetMapping
    ResponseEntity<PageResponse<UserInfo>> list(UserFilterQuery filter, Pageable pageable);

    @Operation(
            summary = "Get a customer by id",
            description = "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No user exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<UserInfo> getById(@Parameter(description = "Customer id") @PathVariable UUID id);

    @Operation(
            summary = "Create a customer",
            description = "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The email or document is already registered to another account.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<UserInfo> create(@Valid @RequestBody CreateUserCommand command);

    @Operation(
            summary = "Update a customer's profile",
            description = "Updates the given user's name and phone numbers. Requires the ATTENDANT or MANAGER role, "
                    + "or the caller updating their own profile."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No user exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}")
    ResponseEntity<UserInfo> update(
            @Parameter(description = "Customer id") @PathVariable UUID id,
            @Valid @RequestBody UpdateUserProfileCommand command
    );

    @Operation(
            summary = "Deactivate a customer",
            description = "Deactivates the given user's Customer facet. Requires the ATTENDANT or MANAGER role, "
                    + "or the caller deactivating their own account."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "204", description = "The customer was deactivated.")
    @ApiResponse(responseCode = "404", description = "No user exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "The user does not have an active Customer facet.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deactivate(@Parameter(description = "Customer id") @PathVariable UUID id);

    @Operation(
            summary = "Reactivate a customer",
            description = "Reactivates a previously deactivated Customer facet. Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "204", description = "The customer was reactivated.")
    @ApiResponse(responseCode = "404", description = "No user exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "The user does not have a Customer facet.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/reactivate")
    ResponseEntity<Void> reactivate(@Parameter(description = "Customer id") @PathVariable UUID id);
}
