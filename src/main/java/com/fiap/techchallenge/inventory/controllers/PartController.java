package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.commands.CreatePartCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdatePartCommand;
import com.fiap.techchallenge.inventory.api.queries.PartFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.shared.responses.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("parts")
@RequiredArgsConstructor
public class PartController {

    private final PartCatalogService partCatalogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<PartInfo>> getAll(
            Pageable pageable,
            PartFilterQuery filter
    ) {
        Page<PartInfo> page = partCatalogService.listParts(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<PartInfo> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(partCatalogService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PartInfo> create(@Valid @RequestBody CreatePartCommand command) {
        PartInfo part = partCatalogService.create(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(part.id())
                .toUri();

        return ResponseEntity.created(location).body(part);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PartInfo> update(@PathVariable UUID id, @Valid @RequestBody UpdatePartCommand command) {
        return ResponseEntity.ok(partCatalogService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        partCatalogService.deactivate(id);

        return ResponseEntity.noContent().build();
    }
}
