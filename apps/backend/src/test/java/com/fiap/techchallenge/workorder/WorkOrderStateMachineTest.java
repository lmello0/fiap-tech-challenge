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
    void finishingDiagnosticsMovesToBudgetInDraft() {
        assertThat(stateMachine.transition(WorkOrderStatus.IN_DIAGNOSTICS, WorkOrderStatus.BUDGET_IN_DRAFT))
                .isEqualTo(WorkOrderStatus.BUDGET_IN_DRAFT);
    }

    @Test
    void budgetInDraftOnlyMovesToWaitingApproval() {
        assertThat(stateMachine.transition(WorkOrderStatus.BUDGET_IN_DRAFT, WorkOrderStatus.WAITING_APPROVAL))
                .isEqualTo(WorkOrderStatus.WAITING_APPROVAL);

        assertThatThrownBy(() -> stateMachine.transition(WorkOrderStatus.IN_DIAGNOSTICS, WorkOrderStatus.WAITING_APPROVAL))
                .isInstanceOf(IllegalWorkOrderTransitionException.class);
    }

    @Test
    void waitingApprovalCanGoEitherWay() {
        assertThat(stateMachine.canTransition(WorkOrderStatus.WAITING_APPROVAL, WorkOrderStatus.APPROVED)).isTrue();
        assertThat(stateMachine.canTransition(WorkOrderStatus.WAITING_APPROVAL, WorkOrderStatus.REFUSED)).isTrue();
    }

    @Test
    void refusalIsTerminalExceptForPickup() {
        assertThat(stateMachine.transition(WorkOrderStatus.REFUSED, WorkOrderStatus.WAITING_PICKUP))
                .isEqualTo(WorkOrderStatus.WAITING_PICKUP);

        assertThatThrownBy(() -> stateMachine.transition(WorkOrderStatus.REFUSED, WorkOrderStatus.IN_PROGRESS))
                .isInstanceOf(IllegalWorkOrderTransitionException.class);
    }

    @Test
    void allowedNextListsEveryDeclaredTransition() {
        assertThat(stateMachine.allowedNext(WorkOrderStatus.WAITING_APPROVAL))
                .containsExactlyInAnyOrder(WorkOrderStatus.APPROVED, WorkOrderStatus.REFUSED, WorkOrderStatus.CANCELLED);

        assertThat(stateMachine.allowedNext(WorkOrderStatus.DELIVERED)).isEmpty();
    }

    @Test
    void onlyDeliveredIsTerminal() {
        assertThat(stateMachine.isTerminal(WorkOrderStatus.DELIVERED)).isTrue();
        assertThat(stateMachine.isTerminal(WorkOrderStatus.RECEIVED)).isFalse();
    }

    @Test
    void cancellableFromEarlyAndLateStatusesAlike() {
        assertThat(stateMachine.transition(WorkOrderStatus.RECEIVED, WorkOrderStatus.CANCELLED))
                .isEqualTo(WorkOrderStatus.CANCELLED);
        assertThat(stateMachine.transition(WorkOrderStatus.WAITING_PICKUP, WorkOrderStatus.CANCELLED))
                .isEqualTo(WorkOrderStatus.CANCELLED);
    }

    @Test
    void cancelledIsTerminalAndUnreachableFromDeliveredOrRefused() {
        assertThat(stateMachine.isTerminal(WorkOrderStatus.CANCELLED)).isTrue();

        assertThatThrownBy(() -> stateMachine.transition(WorkOrderStatus.DELIVERED, WorkOrderStatus.CANCELLED))
                .isInstanceOf(IllegalWorkOrderTransitionException.class);
        assertThatThrownBy(() -> stateMachine.transition(WorkOrderStatus.REFUSED, WorkOrderStatus.CANCELLED))
                .isInstanceOf(IllegalWorkOrderTransitionException.class);
        assertThatThrownBy(() -> stateMachine.transition(WorkOrderStatus.CANCELLED, WorkOrderStatus.RECEIVED))
                .isInstanceOf(IllegalWorkOrderTransitionException.class);
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
