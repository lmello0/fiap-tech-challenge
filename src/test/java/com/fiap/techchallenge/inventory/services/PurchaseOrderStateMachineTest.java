package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;
import com.fiap.techchallenge.inventory.exceptions.IllegalPurchaseOrderStateException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseOrderStateMachineTest {

    private final PurchaseOrderStateMachine stateMachine = new PurchaseOrderStateMachine();

    @Test
    void allowsADeclaredTransition() {
        assertThat(stateMachine.transition(PurchaseOrderStatus.PLACED, PurchaseOrderStatus.RECEIVED))
                .isEqualTo(PurchaseOrderStatus.RECEIVED);
    }

    @Test
    void allowsPartiallyReceivedToStayPartiallyReceived() {
        assertThat(stateMachine.transition(PurchaseOrderStatus.PARTIALLY_RECEIVED, PurchaseOrderStatus.PARTIALLY_RECEIVED))
                .isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
    }

    @Test
    void rejectsATransitionOutOfATerminalStatus() {
        assertThatThrownBy(() -> stateMachine.transition(PurchaseOrderStatus.RECEIVED, PurchaseOrderStatus.CANCELLED))
                .isInstanceOf(IllegalPurchaseOrderStateException.class)
                .hasMessageContaining("RECEIVED")
                .hasMessageContaining("CANCELLED")
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(IllegalPurchaseOrderStateException.class))
                .satisfies(e -> {
                    assertThat(e.getFrom()).isEqualTo(PurchaseOrderStatus.RECEIVED);
                    assertThat(e.getTo()).isEqualTo(PurchaseOrderStatus.CANCELLED);
                });
    }

    @Test
    void rejectsReceivingAnAlreadyCancelledOrder() {
        assertThatThrownBy(() -> stateMachine.transition(PurchaseOrderStatus.CANCELLED, PurchaseOrderStatus.RECEIVED))
                .isInstanceOf(IllegalPurchaseOrderStateException.class);
    }

    @Test
    void bothTerminalStatusesAllowNoFurtherTransitions() {
        assertThat(stateMachine.isTerminal(PurchaseOrderStatus.RECEIVED)).isTrue();
        assertThat(stateMachine.isTerminal(PurchaseOrderStatus.CANCELLED)).isTrue();
        assertThat(stateMachine.isTerminal(PurchaseOrderStatus.PLACED)).isFalse();
        assertThat(stateMachine.isTerminal(PurchaseOrderStatus.PARTIALLY_RECEIVED)).isFalse();
    }
}
