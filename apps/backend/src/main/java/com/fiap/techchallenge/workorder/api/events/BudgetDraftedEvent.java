package com.fiap.techchallenge.workorder.api.events;

import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderSnapshot;

import java.util.UUID;

public record BudgetDraftedEvent(
        UUID workOrderId,
        UUID budgetId,
        EventMetadata metadata,
        WorkOrderSnapshot snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "BUDGET_DRAFTED";
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
