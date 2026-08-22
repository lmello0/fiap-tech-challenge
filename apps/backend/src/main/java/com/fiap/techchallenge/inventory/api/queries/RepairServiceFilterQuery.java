package com.fiap.techchallenge.inventory.api.queries;

public record RepairServiceFilterQuery(
        String code,
        String name,
        Boolean active
) {
}
