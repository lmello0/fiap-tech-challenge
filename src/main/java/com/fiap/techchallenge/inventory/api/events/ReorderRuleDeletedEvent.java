package com.fiap.techchallenge.inventory.api.events;

import com.fiap.techchallenge.inventory.api.representation.ReorderRuleInfo;
import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;

import java.util.UUID;

/** {@code snapshot} is the rule's last state before deletion — the only way it survives at all. */
public record ReorderRuleDeletedEvent(
        UUID partId,
        UUID reorderRuleId,
        EventMetadata metadata,
        ReorderRuleInfo snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "REORDER_RULE_DELETED";
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
        return reorderRuleId;
    }
}
