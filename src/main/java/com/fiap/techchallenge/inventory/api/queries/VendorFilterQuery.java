package com.fiap.techchallenge.inventory.api.queries;

public record VendorFilterQuery(
        String name,
        Boolean active
) {
}
