package com.fiap.techchallenge.user.controllers;

import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.UpdateUserProfileCommand;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Full endpoint documentation lives on {@link UserControllerSwaggerDoc}. */
@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerSwaggerDoc {

    private final UserService service;

    @Override
    public ResponseEntity<UserInfo> me(Jwt jwt) {
        return ResponseEntity.ok(service.getById(UUID.fromString(jwt.getSubject())));
    }

    @Override
    public ResponseEntity<UserInfo> updateMe(Jwt jwt, UpdateUserProfileCommand command) {
        return ResponseEntity.ok(service.updateProfile(UUID.fromString(jwt.getSubject()), command));
    }

    @Override
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<UserInfo> getById(UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }
}
