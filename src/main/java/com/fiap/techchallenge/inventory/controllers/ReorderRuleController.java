package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.ReorderRuleService;
import com.fiap.techchallenge.inventory.api.commands.CreateReorderRuleCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateReorderRuleCommand;
import com.fiap.techchallenge.inventory.api.queries.ReorderRuleFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.ReorderRuleInfo;
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
@RequestMapping("reorder-rules")
@RequiredArgsConstructor
public class ReorderRuleController {

    private final ReorderRuleService reorderRuleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<PageResponse<ReorderRuleInfo>> getAll(
            @PageableDefault Pageable pageable,
            ReorderRuleFilterQuery filter
    ) {
        Page<ReorderRuleInfo> page = reorderRuleService.listReorderRules(filter, pageable);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<ReorderRuleInfo> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(reorderRuleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<ReorderRuleInfo> create(@Valid @RequestBody CreateReorderRuleCommand command) {
        ReorderRuleInfo rule = reorderRuleService.create(command);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(rule.id())
                .toUri();

        return ResponseEntity.created(location).body(rule);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<ReorderRuleInfo> update(@PathVariable UUID id, @Valid @RequestBody UpdateReorderRuleCommand command) {
        return ResponseEntity.ok(reorderRuleService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STOCKIST', 'MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        reorderRuleService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
