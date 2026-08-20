package com.fiap.techchallenge.workorder.api.events;

import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderSnapshot;

import java.util.UUID;

public record BudgetLineFinishedEvent(
        UUID workOrderId,
        UUID budgetLineId,
        EventMetadata metadata,
        WorkOrderSnapshot snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "BUDGET_LINE_FINISHED";
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
        return "BUDGET_LINE";
    }

    @Override
    public UUID entityId() {
        return budgetLineId;
    }
}
