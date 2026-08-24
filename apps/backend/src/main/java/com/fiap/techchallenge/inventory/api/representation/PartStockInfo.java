package com.fiap.techchallenge.inventory.api.representation;

import com.fiap.techchallenge.inventory.enums.StockStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PartStockInfo(
        UUID partId,
        BigDecimal onHand,
        BigDecimal reserved,
        BigDecimal available,
        StockStatus stockStatus,
        BigDecimal avgCost30d,
        BigDecimal avgCost90d,
        BigDecimal avgCost365d,
        BigDecimal avgCostAllTime
) {
}
