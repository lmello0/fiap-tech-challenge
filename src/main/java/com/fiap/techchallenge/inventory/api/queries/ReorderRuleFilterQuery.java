package com.fiap.techchallenge.inventory.api.queries;

import java.util.UUID;

public record ReorderRuleFilterQuery(
        UUID partId,
        UUID vendorId,
        Boolean enabled
) {
}
