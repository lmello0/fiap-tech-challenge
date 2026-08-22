package com.fiap.techchallenge.workorder.api.events;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkOrderWaitingApprovalEventTest {

    @Test
    void carriesTheWorkOrderCustomerAndGrandTotal() {
        UUID workOrderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BigDecimal grandTotal = new BigDecimal("199.90");

        WorkOrderWaitingApprovalEvent event = new WorkOrderWaitingApprovalEvent(workOrderId, customerId, grandTotal);

        assertThat(event.workOrderId()).isEqualTo(workOrderId);
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.grandTotal()).isEqualTo(grandTotal);
    }
}
