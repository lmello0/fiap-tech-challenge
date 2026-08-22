package com.fiap.techchallenge.workorder.api.events;

import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderSnapshot;

import java.util.UUID;

public record WorkOrderDeliveredEvent(
        UUID workOrderId,
        UUID customerId,
        EventMetadata metadata,
        WorkOrderSnapshot snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "WORK_ORDER_DELIVERED";
    }

    @Override
    public String aggregateType() {
        return WorkOrderAggregate.TYPE;
    }

    @Override
    public UUID aggregateId() {
        return workOrderId;
    }
}
