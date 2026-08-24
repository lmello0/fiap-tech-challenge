package com.fiap.techchallenge.inventory.api.queries;

import com.fiap.techchallenge.inventory.enums.StockStatus;

public record PartStockFilterQuery(
        StockStatus stockStatus
) {
}
