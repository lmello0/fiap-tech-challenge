package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.VendorService;
import com.fiap.techchallenge.inventory.api.commands.CreateVendorCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateVendorCommand;
import com.fiap.techchallenge.inventory.api.queries.VendorFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.VendorInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<VendorInfo>> getAll(
            @PageableDefault Pageable pageable,
            VendorFilterQuery filter
    ) {
        Page<VendorInfo> page = vendorService.listVendors(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<VendorInfo> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(vendorService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<VendorInfo> create(@Valid @RequestBody CreateVendorCommand command) {
        VendorInfo vendor = vendorService.create(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(vendor.id())
                .toUri();

        return ResponseEntity.created(location).body(vendor);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<VendorInfo> update(@PathVariable UUID id, @Valid @RequestBody UpdateVendorCommand command) {
        return ResponseEntity.ok(vendorService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        vendorService.deactivate(id);

        return ResponseEntity.noContent().build();
    }
}
