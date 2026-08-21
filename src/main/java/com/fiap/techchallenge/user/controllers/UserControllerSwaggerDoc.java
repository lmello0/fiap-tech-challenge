package com.fiap.techchallenge.user.controllers;

import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import com.fiap.techchallenge.user.api.commands.UpdateUserProfileCommand;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/**
 * Full Swagger/OpenAPI contract for {@link UserController} — the authenticated caller's own
 * profile, plus a worker-only lookup of any user by id.
 */
@Tag(name = "Users", description = "The authenticated caller's own profile, plus worker-only user lookups.")
@RequestMapping("users")
public interface UserControllerSwaggerDoc {

    @Operation(
            summary = "Get my own profile",
            description = "Returns the caller's own profile, resolved from the JWT subject. Any authenticated "
                    + "principal may call this, regardless of role."
    )
    @CommonApiResponses
    @GetMapping("/me")
    ResponseEntity<UserInfo> me(@AuthenticationPrincipal Jwt jwt);

    @Operation(
            summary = "Update my own profile",
            description = "Updates the caller's own name and phone numbers, resolved from the JWT subject. Any "
                    + "authenticated principal may call this, regardless of role."
    )
    @CommonApiResponses
    @PatchMapping("/me")
    ResponseEntity<UserInfo> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileCommand command
    );

    @Operation(
            summary = "Get a user by id",
            description = "Requires the WORKER role (any staff role — ATTENDANT, MECHANIC, STOCKIST, or MANAGER; "
                    + "a CUSTOMER-only principal cannot call this)."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No user exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<UserInfo> getById(@Parameter(description = "User id") @PathVariable UUID id);
}
