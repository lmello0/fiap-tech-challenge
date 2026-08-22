package com.fiap.techchallenge.auth.controllers;

import com.fiap.techchallenge.auth.api.commands.ChangePasswordCommand;
import com.fiap.techchallenge.auth.api.commands.ConfirmEmailChangeCommand;
import com.fiap.techchallenge.auth.api.commands.ConfirmEmailVerificationCommand;
import com.fiap.techchallenge.auth.api.commands.ConfirmPasswordResetCommand;
import com.fiap.techchallenge.auth.api.commands.LoginCommand;
import com.fiap.techchallenge.auth.api.commands.RefreshTokenCommand;
import com.fiap.techchallenge.auth.api.commands.RegisterCustomerCommand;
import com.fiap.techchallenge.auth.api.commands.RegisterWorkerCommand;
import com.fiap.techchallenge.auth.api.commands.RequestEmailChangeCommand;
import com.fiap.techchallenge.auth.api.commands.RequestPasswordResetCommand;
import com.fiap.techchallenge.auth.api.commands.ResendEmailVerificationCommand;
import com.fiap.techchallenge.auth.api.representation.TokenResponse;
import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Full Swagger/OpenAPI contract for {@link AuthController} — registration, login, session
 * (refresh/logout), password, and email-change/verification flows. Most of these endpoints are
 * public by design (see {@code SecurityConfig}'s permitAll list); each says so explicitly below via
 * an empty {@code @SecurityRequirements}, which overrides the global bearer-JWT default.
 */
@Tag(name = "Auth", description = "Registration, login, session, password, and email verification/change flows.")
@RequestMapping("auth")
public interface AuthControllerSwaggerDoc {

    @Operation(
            summary = "Register a customer",
            description = "Creates a new CUSTOMER account and returns an access/refresh token pair — no login call "
                    + "needed afterwards. Public; no authentication required."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The email or document is already registered to another account.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/register/customer")
    ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterCustomerCommand command);

    @Operation(
            summary = "Register a worker",
            description = "Onboards a new staff account (ATTENDANT, MECHANIC, STOCKIST, or MANAGER) with a "
                    + "temporary password that must be rotated on first use. Requires the MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "409", description = "The email or document is already registered to another account.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/register/worker")
    ResponseEntity<UserInfo> registerWorker(@Valid @RequestBody RegisterWorkerCommand command);

    @Operation(
            summary = "Log in",
            description = "Exchanges an email and password for an access/refresh token pair. Public; no "
                    + "authentication required."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "The email/password combination is invalid.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "The account is locked (too many failed attempts), disabled, or its email has not been verified yet.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/login")
    ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginCommand command);

    @Operation(
            summary = "Refresh an access token",
            description = "Rotates a refresh token for a new access/refresh token pair. The old refresh token is "
                    + "invalidated; reusing it revokes every session for the account. Public; no authentication required."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "The refresh token is unknown, expired, already used, or revoked.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "The account is disabled.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/refresh-token")
    ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenCommand command);

    @Operation(
            summary = "Log out",
            description = "Revokes the given refresh token, ending that one session. Idempotent — an already-revoked "
                    + "or unknown token is not an error. Public; no authentication required (the refresh token itself "
                    + "is the credential being acted on)."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "204", description = "The refresh token was revoked, or was already invalid/unknown.")
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenCommand command);

    @Operation(
            summary = "Log out everywhere",
            description = "Revokes every refresh token for the caller's account, ending all sessions on every device. "
                    + "Requires a valid JWT; no specific role."
    )
    @ApiResponse(responseCode = "204", description = "Every session for the account was revoked.")
    @ApiResponse(responseCode = "401", description = "No valid JWT bearer token was supplied.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/logout-all")
    ResponseEntity<Void> logoutEverywhere(@AuthenticationPrincipal Jwt jwt);

    @Operation(
            summary = "Change password",
            description = "Changes the caller's own password, given the current one, and revokes every existing "
                    + "session. Requires a valid JWT; no specific role."
    )
    @ApiResponse(responseCode = "204", description = "The password was changed and every existing session revoked.")
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "No valid JWT bearer token was supplied, or the current password is wrong.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/password/change")
    ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordCommand command);

    @Operation(
            summary = "Request a password reset",
            description = "Emails a password-reset link if the address belongs to an account. Always answers 204 "
                    + "regardless, so as not to reveal whether the address is registered. Public; no authentication required."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "204", description = "Always returned, whether or not the address belongs to an account.")
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/password/reset")
    ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody RequestPasswordResetCommand command);

    @Operation(
            summary = "Confirm a password reset",
            description = "Sets a new password using the token emailed by the reset request, and revokes every "
                    + "existing session. Public; no authentication required."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "204", description = "The password was reset and every existing session revoked.")
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "The token is invalid, expired, or already used.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/password/reset/confirm")
    ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody ConfirmPasswordResetCommand command);

    @Operation(
            summary = "Resend the email verification link",
            description = "Re-issues the verification email if the address belongs to an account that is not yet "
                    + "verified. Always answers 204 regardless, so as not to reveal whether the address is registered. "
                    + "Public; no authentication required."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "204", description = "Always returned, whether or not the address belongs to an unverified account.")
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/email-verification/resend")
    ResponseEntity<Void> resendEmailVerification(@Valid @RequestBody ResendEmailVerificationCommand command);

    @Operation(
            summary = "Confirm email verification",
            description = "Marks the account's email as verified using the token from the verification email. "
                    + "Public; no authentication required."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "204", description = "The email was marked verified.")
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "The token is invalid, expired, or already used.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/email-verification/confirm")
    ResponseEntity<Void> confirmEmailVerification(@Valid @RequestBody ConfirmEmailVerificationCommand command);

    @Operation(
            summary = "Request an email change",
            description = "Emails a confirmation link to the new address; the account's email is not changed until "
                    + "that link is confirmed. Requires a valid JWT; no specific role."
    )
    @ApiResponse(responseCode = "204", description = "The confirmation link was sent to the new address.")
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "No valid JWT bearer token was supplied.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/email-change")
    ResponseEntity<Void> requestEmailChange(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RequestEmailChangeCommand command);

    @Operation(
            summary = "Confirm an email change",
            description = "Applies the pending email change using the token from the confirmation email, and revokes "
                    + "every existing session. Public; no authentication required (the token itself is the credential "
                    + "being acted on)."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "204", description = "The email change was applied and every existing session revoked.")
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "The token is invalid, expired, or already used.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The new email address was taken by another account between the request and this confirmation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/email-change/confirm")
    ResponseEntity<Void> confirmEmailChange(@Valid @RequestBody ConfirmEmailChangeCommand command);
}
