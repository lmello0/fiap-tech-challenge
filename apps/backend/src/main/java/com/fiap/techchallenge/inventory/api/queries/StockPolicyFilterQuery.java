package com.fiap.techchallenge.inventory.api.queries;

import java.util.UUID;

public record StockPolicyFilterQuery(
        UUID partId,
        UUID vendorId,
        Boolean autoReorderEnabled
) {
}
