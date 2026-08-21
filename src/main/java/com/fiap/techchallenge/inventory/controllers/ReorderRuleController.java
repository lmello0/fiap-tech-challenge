package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.ReorderRuleService;
import com.fiap.techchallenge.inventory.api.commands.CreateReorderRuleCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateReorderRuleCommand;
import com.fiap.techchallenge.inventory.api.queries.ReorderRuleFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.ReorderRuleInfo;
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

/** Full endpoint documentation lives on {@link ReorderRuleControllerSwaggerDoc}. */
@RestController
@RequiredArgsConstructor
public class ReorderRuleController implements ReorderRuleControllerSwaggerDoc {

    private final ReorderRuleService reorderRuleService;

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<ReorderRuleInfo>> getAll(Pageable pageable, ReorderRuleFilterQuery filter) {
        Page<ReorderRuleInfo> page = reorderRuleService.listReorderRules(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<ReorderRuleInfo> getById(UUID id) {
        return ResponseEntity.ok(reorderRuleService.getById(id));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<ReorderRuleInfo> create(CreateReorderRuleCommand command) {
        ReorderRuleInfo rule = reorderRuleService.create(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(rule.id())
                .toUri();

        return ResponseEntity.created(location).body(rule);
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<ReorderRuleInfo> update(UUID id, UpdateReorderRuleCommand command) {
        return ResponseEntity.ok(reorderRuleService.update(id, command));
    }

    @Override
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<Void> delete(UUID id) {
        reorderRuleService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
