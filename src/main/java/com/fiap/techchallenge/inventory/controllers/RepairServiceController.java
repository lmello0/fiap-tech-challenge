package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.RepairServiceCatalogService;
import com.fiap.techchallenge.inventory.api.commands.CreateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.queries.RepairServiceFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
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
@RequestMapping("services")
@RequiredArgsConstructor
public class RepairServiceController {

    private final RepairServiceCatalogService repairServiceCatalogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<RepairServiceInfo>> getAll(
            @PageableDefault Pageable pageable,
            RepairServiceFilterQuery filter
    ) {
        Page<RepairServiceInfo> page = repairServiceCatalogService.listServices(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MECHANIC', 'STOCKIST', 'MANAGER')")
    public ResponseEntity<RepairServiceInfo> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(repairServiceCatalogService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<RepairServiceInfo> create(@Valid @RequestBody CreateRepairServiceCommand command) {
        RepairServiceInfo service = repairServiceCatalogService.create(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(service.id())
                .toUri();

        return ResponseEntity.created(location).body(service);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<RepairServiceInfo> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRepairServiceCommand command
    ) {
        return ResponseEntity.ok(repairServiceCatalogService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        repairServiceCatalogService.deactivate(id);

        return ResponseEntity.noContent().build();
    }
}
