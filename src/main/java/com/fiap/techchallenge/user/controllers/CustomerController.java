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

import java.util.UUID;

@RestController
@RequestMapping("customers")
@RequiredArgsConstructor
public class CustomerController {

    private final UserService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<PageResponse<UserInfo>> list(UserFilterQuery filter, Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.listCustomers(filter, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<UserInfo> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER') or #id.toString() == authentication.name")
    public ResponseEntity<UserInfo> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserProfileCommand command) {
        return ResponseEntity.ok(service.updateProfile(id, command));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER') or #id.toString() == authentication.name")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivateCustomer(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<Void> reactivate(@PathVariable UUID id) {
        service.reactivateCustomer(id);

        return ResponseEntity.noContent().build();
    }
}
