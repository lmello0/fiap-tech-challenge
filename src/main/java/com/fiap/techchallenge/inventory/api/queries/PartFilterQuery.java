package com.fiap.techchallenge.inventory.api.queries;

import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;

import java.util.List;

public record PartFilterQuery(
        String sku,
        String name,
        String brand,
        List<UnitOfMeasure> unitOfMeasures,
        Boolean active
) {
}
