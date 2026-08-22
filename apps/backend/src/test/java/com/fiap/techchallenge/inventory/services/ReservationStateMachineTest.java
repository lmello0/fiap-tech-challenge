package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.enums.ReservationStatus;
import com.fiap.techchallenge.inventory.exceptions.IllegalReservationTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationStateMachineTest {

    private final ReservationStateMachine stateMachine = new ReservationStateMachine();

    @Test
    void allowsADeclaredTransition() {
        assertThat(stateMachine.transition(ReservationStatus.HELD, ReservationStatus.CONSUMED))
                .isEqualTo(ReservationStatus.CONSUMED);
    }

    @Test
    void rejectsATransitionOutOfATerminalStatus() {
        assertThatThrownBy(() -> stateMachine.transition(ReservationStatus.CONSUMED, ReservationStatus.RELEASED))
                .isInstanceOf(IllegalReservationTransitionException.class)
                .hasMessageContaining("CONSUMED")
                .hasMessageContaining("RELEASED")
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(IllegalReservationTransitionException.class))
                .satisfies(e -> {
                    assertThat(e.getFrom()).isEqualTo(ReservationStatus.CONSUMED);
                    assertThat(e.getTo()).isEqualTo(ReservationStatus.RELEASED);
                });
    }

    @Test
    void everyTerminalStatusAllowsNoFurtherTransitions() {
        assertThat(stateMachine.isTerminal(ReservationStatus.CONSUMED)).isTrue();
        assertThat(stateMachine.isTerminal(ReservationStatus.RELEASED)).isTrue();
        assertThat(stateMachine.isTerminal(ReservationStatus.EXPIRED)).isTrue();
        assertThat(stateMachine.isTerminal(ReservationStatus.HELD)).isFalse();
    }

    @Test
    void heldCanResolveToAnyOfTheThreeTerminalStatuses() {
        assertThat(stateMachine.allowedNext(ReservationStatus.HELD))
                .containsExactlyInAnyOrder(ReservationStatus.CONSUMED, ReservationStatus.RELEASED, ReservationStatus.EXPIRED);
    }
}
