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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Registration, login, session, password, and email-change/verification endpoints. Full endpoint
 * documentation lives on {@link AuthControllerSwaggerDoc}.
 */
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerSwaggerDoc {

    private final AuthService service;

    @Override
    public ResponseEntity<TokenResponse> register(RegisterCustomerCommand command) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.registerCustomer(command));
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UserInfo> registerWorker(RegisterWorkerCommand command) {
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

    @Override
    public ResponseEntity<TokenResponse> login(LoginCommand command) {
        return ResponseEntity.ok(service.login(command));
    }

    @Override
    public ResponseEntity<TokenResponse> refresh(RefreshTokenCommand command) {
        return ResponseEntity.ok(service.refresh(command.refreshToken()));
    }

    @Override
    public ResponseEntity<Void> logout(RefreshTokenCommand command) {
        service.logout(command.refreshToken());

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> logoutEverywhere(Jwt jwt) {
        service.logoutEverywhere(UUID.fromString(jwt.getSubject()));

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> changePassword(Jwt jwt, ChangePasswordCommand command) {
        service.changePassword(UUID.fromString(jwt.getSubject()), command);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> requestPasswordReset(RequestPasswordResetCommand command) {
        service.requestPasswordReset(command);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> confirmPasswordReset(ConfirmPasswordResetCommand command) {
        service.confirmPasswordReset(command);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> resendEmailVerification(ResendEmailVerificationCommand command) {
        service.resendEmailVerification(command);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> confirmEmailVerification(ConfirmEmailVerificationCommand command) {
        service.confirmEmailVerification(command);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> requestEmailChange(Jwt jwt, RequestEmailChangeCommand command) {
        service.requestEmailChange(UUID.fromString(jwt.getSubject()), command);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> confirmEmailChange(ConfirmEmailChangeCommand command) {
        service.confirmEmailChange(command);

        return ResponseEntity.noContent().build();
    }
}
