package com.fiap.techchallenge.user.controllers;

import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.UpdateUserProfileCommand;
import com.fiap.techchallenge.user.api.queries.UserFilterQuery;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/** Full endpoint documentation lives on {@link WorkerControllerSwaggerDoc}. */
@RestController
@RequiredArgsConstructor
public class WorkerController implements WorkerControllerSwaggerDoc {

    private final UserService service;

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<PageResponse<UserInfo>> list(UserFilterQuery filter, Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.listWorkers(filter, pageable)));
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UserInfo> getById(UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @Override
    @PreAuthorize("#id.toString() == authentication.name")
    public ResponseEntity<UserInfo> update(UUID id, UpdateUserProfileCommand command) {
        return ResponseEntity.ok(service.updateProfile(id, command));
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> terminate(UUID id) {
        service.terminateWorker(id, LocalDate.now());

        return ResponseEntity.noContent().build();
    }
}
