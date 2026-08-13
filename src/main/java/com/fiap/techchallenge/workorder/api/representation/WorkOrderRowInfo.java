package com.fiap.techchallenge.workorder.api.representation;

import com.fiap.techchallenge.workorder.enums.RowType;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkOrderRowInfo(
        RowType type,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        UUID partId
) {
}
