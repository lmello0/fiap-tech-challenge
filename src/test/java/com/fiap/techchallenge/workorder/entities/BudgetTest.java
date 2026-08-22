package com.fiap.techchallenge.workorder.entities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetTest {

    @Test
    void getWorkOrderIdIsNullWhenNoWorkOrderIsLinkedYet() {
        Budget budget = new Budget();

        assertThat(budget.getWorkOrderId()).isNull();
    }

    @Test
    void getWorkOrderIdDelegatesToTheLinkedWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        UUID workOrderId = UUID.randomUUID();
        workOrder.setId(workOrderId);

        Budget budget = new Budget();
        budget.setWorkOrder(workOrder);

        assertThat(budget.getWorkOrderId()).isEqualTo(workOrderId);
    }
}
