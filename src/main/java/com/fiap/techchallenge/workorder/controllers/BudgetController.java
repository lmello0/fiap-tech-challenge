package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.workorder.api.BudgetService;
import com.fiap.techchallenge.workorder.api.commands.AddBudgetLineCommand;
import com.fiap.techchallenge.workorder.api.commands.ChangeBudgetLineQuantityCommand;
import com.fiap.techchallenge.workorder.api.representation.BudgetInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Staff-only budget draft editing and send lifecycle (ADR 0008/0009/0010). */
@RestController
@RequestMapping("budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MECHANIC', 'MANAGER')")
    public ResponseEntity<BudgetInfo> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<BudgetInfo> addLine(@PathVariable UUID id, @Valid @RequestBody AddBudgetLineCommand command) {
        return ResponseEntity.ok(service.addLine(id, command));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<BudgetInfo> removeLine(@PathVariable UUID id, @PathVariable UUID lineId) {
        return ResponseEntity.ok(service.removeLine(id, lineId));
    }

    @PatchMapping("/{id}/lines/{lineId}/quantity")
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<BudgetInfo> changeLineQuantity(
            @PathVariable UUID id,
            @PathVariable UUID lineId,
            @Valid @RequestBody ChangeBudgetLineQuantityCommand command
    ) {
        return ResponseEntity.ok(service.changeLineQuantity(id, lineId, command));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<BudgetInfo> send(@PathVariable UUID id) {
        return ResponseEntity.ok(service.send(id));
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<BudgetInfo> resend(@PathVariable UUID id) {
        return ResponseEntity.ok(service.resend(id));
    }
}
