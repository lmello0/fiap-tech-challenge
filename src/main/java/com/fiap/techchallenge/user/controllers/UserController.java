package com.fiap.techchallenge.user.controllers;

import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.UpdateUserProfileCommand;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @GetMapping("/me")
    public ResponseEntity<UserInfo> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.getById(UUID.fromString(jwt.getSubject())));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserInfo> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileCommand command
    ) {
        return ResponseEntity.ok(service.updateProfile(UUID.fromString(jwt.getSubject()), command));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<UserInfo> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }
}
