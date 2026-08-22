package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.enums.ReservationStatus;
import com.fiap.techchallenge.inventory.exceptions.IllegalReservationTransitionException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class ReservationStateMachine {

    private static final Map<ReservationStatus, Set<ReservationStatus>> TRANSITIONS = Map.of(
            ReservationStatus.HELD, EnumSet.of(
                    ReservationStatus.CONSUMED,
                    ReservationStatus.RELEASED,
                    ReservationStatus.EXPIRED),
            ReservationStatus.CONSUMED, EnumSet.noneOf(ReservationStatus.class),
            ReservationStatus.RELEASED, EnumSet.noneOf(ReservationStatus.class),
            ReservationStatus.EXPIRED, EnumSet.noneOf(ReservationStatus.class)
    );

    public boolean canTransition(ReservationStatus from, ReservationStatus to) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(ReservationStatus.class)).contains(to);
    }

    public ReservationStatus transition(ReservationStatus from, ReservationStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalReservationTransitionException(from, to);
        }

        return to;
    }

    public Set<ReservationStatus> allowedNext(ReservationStatus from) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(ReservationStatus.class));
    }

    public boolean isTerminal(ReservationStatus status) {
        return allowedNext(status).isEmpty();
    }
}
