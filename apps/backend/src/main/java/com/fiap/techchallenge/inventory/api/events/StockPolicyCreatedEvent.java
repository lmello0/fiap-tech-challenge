package com.fiap.techchallenge.inventory.api.events;

import com.fiap.techchallenge.inventory.api.representation.StockPolicyInfo;
import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;

import java.util.UUID;

/** Belongs to the owning Part's Timeline — a Reorder Rule is a per-part standing instruction. */
public record StockPolicyCreatedEvent(
        UUID partId,
        UUID stockPolicyId,
        EventMetadata metadata,
        StockPolicyInfo snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "REORDER_RULE_CREATED";
    }

    @Override
    public String aggregateType() {
        return InventoryAggregates.PART;
    }

    @Override
    public UUID aggregateId() {
        return partId;
    }

    @Override
    public String entityType() {
        return "REORDER_RULE";
    }

    @Override
    public UUID entityId() {
        return stockPolicyId;
    }
}
