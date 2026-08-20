package com.fiap.techchallenge.workorder.api;

import com.fiap.techchallenge.workorder.api.commands.AddBudgetLineCommand;
import com.fiap.techchallenge.workorder.api.commands.ChangeBudgetLineQuantityCommand;
import com.fiap.techchallenge.workorder.api.commands.RefuseWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.representation.BudgetInfo;

import java.util.UUID;

/**
 * Budget draft editing and send/approve/refuse lifecycle (ADR 0008/0009/0010). A Budget's lines are
 * only editable while it is DRAFT; each edit reserves or releases only its delta.
 */
public interface BudgetService {

    BudgetInfo getById(UUID budgetId);

    BudgetInfo addLine(UUID budgetId, AddBudgetLineCommand command);

    BudgetInfo removeLine(UUID budgetId, UUID lineId);

    BudgetInfo changeLineQuantity(UUID budgetId, UUID lineId, ChangeBudgetLineQuantityCommand command);

    /** Locks the budget's lines and moves DRAFT -> WAITING_SEND, triggering async email delivery. */
    BudgetInfo send(UUID budgetId);

    /** Re-triggers delivery for a Budget stuck in WAITING_SEND, without re-locking or re-reserving. */
    BudgetInfo resend(UUID budgetId);

    /** Customer decision: SENT -> APPROVED, and the work order moves WAITING_APPROVAL -> APPROVED. */
    BudgetInfo approve(UUID budgetId, UUID customerId);

    /** Customer decision: SENT -> REFUSED. Terminal — no requoting. */
    BudgetInfo refuse(UUID budgetId, UUID customerId, RefuseWorkOrderCommand command);
}
