package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.VendorService;
import com.fiap.techchallenge.inventory.api.commands.CreateVendorCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateVendorCommand;
import com.fiap.techchallenge.inventory.api.queries.VendorFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.VendorInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** Full endpoint documentation lives on {@link VendorControllerSwaggerDoc}. */
@RestController
@RequiredArgsConstructor
public class VendorController implements VendorControllerSwaggerDoc {

    private final VendorService vendorService;

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<VendorInfo>> getAll(Pageable pageable, VendorFilterQuery filter) {
        Page<VendorInfo> page = vendorService.listVendors(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<VendorInfo> getById(UUID id) {
        return ResponseEntity.ok(vendorService.getById(id));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<VendorInfo> create(CreateVendorCommand command) {
        VendorInfo vendor = vendorService.create(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(vendor.id())
                .toUri();

        return ResponseEntity.created(location).body(vendor);
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<VendorInfo> update(UUID id, UpdateVendorCommand command) {
        return ResponseEntity.ok(vendorService.update(id, command));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<Void> deactivate(UUID id) {
        vendorService.deactivate(id);

        return ResponseEntity.noContent().build();
    }
}
