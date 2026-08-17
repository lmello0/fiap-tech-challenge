package com.fiap.techchallenge.user.controllers;

import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.UpdateUserProfileCommand;
import com.fiap.techchallenge.user.api.queries.UserFilterQuery;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("workers")
@RequiredArgsConstructor
public class WorkerController {

    private final UserService service;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<PageResponse<UserInfo>> list(UserFilterQuery filter, Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.listWorkers(filter, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UserInfo> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("#id.toString() == authentication.name")
    public ResponseEntity<UserInfo> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserProfileCommand command) {
        return ResponseEntity.ok(service.updateProfile(id, command));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> terminate(@PathVariable UUID id) {
        service.terminateWorker(id, LocalDate.now());

        return ResponseEntity.noContent().build();
    }
}
