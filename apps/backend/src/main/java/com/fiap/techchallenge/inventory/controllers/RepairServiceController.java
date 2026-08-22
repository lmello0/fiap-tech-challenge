package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.RepairServiceCatalogService;
import com.fiap.techchallenge.inventory.api.commands.CreateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.queries.RepairServiceFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
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

/** Full endpoint documentation lives on {@link RepairServiceControllerSwaggerDoc}. */
@RestController
@RequiredArgsConstructor
public class RepairServiceController implements RepairServiceControllerSwaggerDoc {

    private final RepairServiceCatalogService repairServiceCatalogService;

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<RepairServiceInfo>> getAll(Pageable pageable, RepairServiceFilterQuery filter) {
        Page<RepairServiceInfo> page = repairServiceCatalogService.listServices(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<RepairServiceInfo> getById(UUID id) {
        return ResponseEntity.ok(repairServiceCatalogService.getById(id));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<RepairServiceInfo> create(CreateRepairServiceCommand command) {
        RepairServiceInfo service = repairServiceCatalogService.create(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(service.id())
                .toUri();

        return ResponseEntity.created(location).body(service);
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<RepairServiceInfo> update(UUID id, UpdateRepairServiceCommand command) {
        return ResponseEntity.ok(repairServiceCatalogService.update(id, command));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<Void> deactivate(UUID id) {
        repairServiceCatalogService.deactivate(id);

        return ResponseEntity.noContent().build();
    }
}
