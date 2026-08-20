package com.fiap.techchallenge.workorder.api.events;

import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderSnapshot;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetLineQuantityChangedEvent(
        UUID workOrderId,
        UUID budgetId,
        UUID budgetLineId,
        BigDecimal oldQuantity,
        BigDecimal newQuantity,
        EventMetadata metadata,
        WorkOrderSnapshot snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "BUDGET_LINE_QUANTITY_CHANGED";
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
