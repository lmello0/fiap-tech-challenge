package com.fiap.techchallenge.workorder;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import com.fiap.techchallenge.workorder.exceptions.IllegalWorkOrderTransitionException;
import com.fiap.techchallenge.workorder.services.WorkOrderStateMachine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkOrderStateMachineTest {

    private final WorkOrderStateMachine stateMachine = new WorkOrderStateMachine();

    @Test
    void allowsADeclaredTransition() {
        assertThat(stateMachine.transition(WorkOrderStatus.APPROVED, WorkOrderStatus.IN_PROGRESS))
                .isEqualTo(WorkOrderStatus.IN_PROGRESS);
    }

    @Test
    void rejectsATransitionOutOfATerminalStatus() {
        assertThatThrownBy(() -> stateMachine.transition(WorkOrderStatus.DELIVERED, WorkOrderStatus.IN_PROGRESS))
                .isInstanceOf(IllegalWorkOrderTransitionException.class)
                .hasMessageContaining("DELIVERED")
                .hasMessageContaining("IN_PROGRESS")
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(IllegalWorkOrderTransitionException.class))
                .satisfies(e -> {
                    assertThat(e.getFrom()).isEqualTo(WorkOrderStatus.DELIVERED);
                    assertThat(e.getTo()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
                });
    }
}
