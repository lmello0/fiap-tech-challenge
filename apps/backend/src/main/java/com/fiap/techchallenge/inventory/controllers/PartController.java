package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.commands.CreatePartCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdatePartCommand;
import com.fiap.techchallenge.inventory.api.queries.PartFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
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

/** Full endpoint documentation lives on {@link PartControllerSwaggerDoc}. */
@RestController
@RequiredArgsConstructor
public class PartController implements PartControllerSwaggerDoc {

    private final PartCatalogService partCatalogService;

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<PartInfo>> getAll(Pageable pageable, PartFilterQuery filter) {
        Page<PartInfo> page = partCatalogService.listParts(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<PartInfo> getById(UUID id) {
        return ResponseEntity.ok(partCatalogService.getById(id));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PartInfo> create(CreatePartCommand command) {
        PartInfo part = partCatalogService.create(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(part.id())
                .toUri();

        return ResponseEntity.created(location).body(part);
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PartInfo> update(UUID id, UpdatePartCommand command) {
        return ResponseEntity.ok(partCatalogService.update(id, command));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<Void> deactivate(UUID id) {
        partCatalogService.deactivate(id);

        return ResponseEntity.noContent().build();
    }
}
