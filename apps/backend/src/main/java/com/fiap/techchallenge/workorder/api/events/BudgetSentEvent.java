package com.fiap.techchallenge.workorder.api.events;

import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderSnapshot;

import java.math.BigDecimal;
import java.util.UUID;

/** Published once delivery of the budget email is confirmed (Budget WAITING_SEND -> SENT). */
public record BudgetSentEvent(
        UUID workOrderId,
        UUID budgetId,
        UUID customerId,
        BigDecimal grandTotal,
        EventMetadata metadata,
        WorkOrderSnapshot snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "BUDGET_SENT";
    }

    @Override
    public String aggregateType() {
        return WorkOrderAggregate.TYPE;
    }

    @Override
    public UUID aggregateId() {
        return workOrderId;
    }

    @Override
    public String entityType() {
        return "BUDGET";
    }

    @Override
    public UUID entityId() {
        return budgetId;
    }
}
