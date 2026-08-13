package com.fiap.techchallenge.workorder.api.events;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkOrderWaitingApprovalEvent(
        UUID workOrderId,
        UUID customerId,
        BigDecimal grandTotal
) {
}
