package com.fiap.techchallenge.workorder.api.events;

import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderSnapshot;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkOrderApprovedEvent(
        UUID workOrderId,
        UUID customerId,
        BigDecimal grandTotal,
        EventMetadata metadata,
        WorkOrderSnapshot snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "WORK_ORDER_APPROVED";
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
