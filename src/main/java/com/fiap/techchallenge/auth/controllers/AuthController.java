package com.fiap.techchallenge.auth.controllers;

import com.fiap.techchallenge.auth.api.AuthService;
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
import com.fiap.techchallenge.user.api.representation.UserInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register/customer")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterCustomerCommand command) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.registerCustomer(command));
    }

    @PostMapping("/register/worker")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UserInfo> registerWorker(@Valid @RequestBody RegisterWorkerCommand command) {
        UserInfo userInfo = service.registerWorker(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/users/{id}")
                .buildAndExpand(userInfo.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(userInfo);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginCommand command) {
        return ResponseEntity.ok(service.login(command));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenCommand command) {
        return ResponseEntity.ok(service.refresh(command.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenCommand command) {
        service.logout(command.refreshToken());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutEverywhere(@AuthenticationPrincipal Jwt jwt) {
        service.logoutEverywhere(UUID.fromString(jwt.getSubject()));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordCommand command
    ) {
        service.changePassword(UUID.fromString(jwt.getSubject()), command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody RequestPasswordResetCommand command) {
        service.requestPasswordReset(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody ConfirmPasswordResetCommand command) {
        service.confirmPasswordReset(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-verification/resend")
    public ResponseEntity<Void> resendEmailVerification(@Valid @RequestBody ResendEmailVerificationCommand command) {
        service.resendEmailVerification(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<Void> confirmEmailVerification(@Valid @RequestBody ConfirmEmailVerificationCommand command) {
        service.confirmEmailVerification(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-change")
    public ResponseEntity<Void> requestEmailChange(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RequestEmailChangeCommand command
    ) {
        service.requestEmailChange(UUID.fromString(jwt.getSubject()), command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-change/confirm")
    public ResponseEntity<Void> confirmEmailChange(@Valid @RequestBody ConfirmEmailChangeCommand command) {
        service.confirmEmailChange(command);

        return ResponseEntity.noContent().build();
    }
}
