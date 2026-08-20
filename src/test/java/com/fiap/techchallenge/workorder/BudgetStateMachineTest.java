package com.fiap.techchallenge.workorder;

import com.fiap.techchallenge.workorder.enums.BudgetStatus;
import com.fiap.techchallenge.workorder.exceptions.IllegalBudgetTransitionException;
import com.fiap.techchallenge.workorder.services.BudgetStateMachine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetStateMachineTest {

    private final BudgetStateMachine stateMachine = new BudgetStateMachine();

    @Test
    void draftMovesToWaitingSend() {
        assertThat(stateMachine.transition(BudgetStatus.DRAFT, BudgetStatus.WAITING_SEND))
                .isEqualTo(BudgetStatus.WAITING_SEND);
    }

    @Test
    void waitingSendMovesToSentOnly() {
        assertThat(stateMachine.transition(BudgetStatus.WAITING_SEND, BudgetStatus.SENT))
                .isEqualTo(BudgetStatus.SENT);

        assertThatThrownBy(() -> stateMachine.transition(BudgetStatus.WAITING_SEND, BudgetStatus.APPROVED))
                .isInstanceOf(IllegalBudgetTransitionException.class);
    }

    @Test
    void sentCanBeApprovedOrRefused() {
        assertThat(stateMachine.canTransition(BudgetStatus.SENT, BudgetStatus.APPROVED)).isTrue();
        assertThat(stateMachine.canTransition(BudgetStatus.SENT, BudgetStatus.REFUSED)).isTrue();
    }

    @Test
    void approvedAndRefusedAreTerminal() {
        assertThatThrownBy(() -> stateMachine.transition(BudgetStatus.APPROVED, BudgetStatus.SENT))
                .isInstanceOf(IllegalBudgetTransitionException.class);
        assertThatThrownBy(() -> stateMachine.transition(BudgetStatus.REFUSED, BudgetStatus.SENT))
                .isInstanceOf(IllegalBudgetTransitionException.class);
    }

    @Test
    void draftCannotSkipStraightToSent() {
        assertThatThrownBy(() -> stateMachine.transition(BudgetStatus.DRAFT, BudgetStatus.SENT))
                .isInstanceOf(IllegalBudgetTransitionException.class);
    }
}
