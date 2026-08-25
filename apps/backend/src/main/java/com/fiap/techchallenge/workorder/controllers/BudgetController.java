package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.workorder.api.BudgetService;
import com.fiap.techchallenge.workorder.api.commands.AddBudgetLineCommand;
import com.fiap.techchallenge.workorder.api.commands.BudgetTokenCommand;
import com.fiap.techchallenge.workorder.api.commands.BudgetTokenRefusalCommand;
import com.fiap.techchallenge.workorder.api.commands.ChangeBudgetLineQuantityCommand;
import com.fiap.techchallenge.workorder.api.commands.RefuseWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.representation.BudgetInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Staff-only budget draft editing and send lifecycle (ADR 0008/0009/0010), plus the public,
 * token-authenticated Budget decision endpoints (ADR 0021) a customer reaches from the Budget email
 * without signing in. Full endpoint documentation lives on {@link BudgetControllerSwaggerDoc}.
 */
@RestController
@RequiredArgsConstructor
public class BudgetController implements BudgetControllerSwaggerDoc {

    private final BudgetService service;

    @Override
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MECHANIC', 'MANAGER')")
    public ResponseEntity<BudgetInfo> getById(UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<BudgetInfo> addLine(UUID id, AddBudgetLineCommand command) {
        return ResponseEntity.ok(service.addLine(id, command));
    }

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<BudgetInfo> removeLine(UUID id, UUID lineId) {
        return ResponseEntity.ok(service.removeLine(id, lineId));
    }

    @Override
    @PreAuthorize("hasAnyRole('MECHANIC', 'MANAGER')")
    public ResponseEntity<BudgetInfo> changeLineQuantity(UUID id, UUID lineId, ChangeBudgetLineQuantityCommand command) {
        return ResponseEntity.ok(service.changeLineQuantity(id, lineId, command));
    }

    @Override
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<BudgetInfo> send(UUID id) {
        return ResponseEntity.ok(service.send(id));
    }

    @Override
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MANAGER')")
    public ResponseEntity<BudgetInfo> resend(UUID id) {
        return ResponseEntity.ok(service.resend(id));
    }

    @Override
    public ResponseEntity<BudgetInfo> viewByToken(BudgetTokenCommand command) {
        return ResponseEntity.ok(service.viewByToken(command.token()));
    }

    @Override
    public ResponseEntity<BudgetInfo> approveByToken(BudgetTokenCommand command) {
        return ResponseEntity.ok(service.approveByToken(command.token()));
    }

    @Override
    public ResponseEntity<BudgetInfo> refuseByToken(BudgetTokenRefusalCommand command) {
        return ResponseEntity.ok(service.refuseByToken(command.token(), new RefuseWorkOrderCommand(command.reason())));
    }
}
