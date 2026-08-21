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

import java.util.UUID;

/** Full endpoint documentation lives on {@link CustomerControllerSwaggerDoc}. */
@RestController
@RequiredArgsConstructor
public class CustomerController implements CustomerControllerSwaggerDoc {

    private final UserService service;

    @Override
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<PageResponse<UserInfo>> list(UserFilterQuery filter, Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.listCustomers(filter, pageable)));
    }

    @Override
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<UserInfo> getById(UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @Override
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER') or #id.toString() == authentication.name")
    public ResponseEntity<UserInfo> update(UUID id, UpdateUserProfileCommand command) {
        return ResponseEntity.ok(service.updateProfile(id, command));
    }

    @Override
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER') or #id.toString() == authentication.name")
    public ResponseEntity<Void> deactivate(UUID id) {
        service.deactivateCustomer(id);

        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<Void> reactivate(UUID id) {
        service.reactivateCustomer(id);

        return ResponseEntity.noContent().build();
    }
}
