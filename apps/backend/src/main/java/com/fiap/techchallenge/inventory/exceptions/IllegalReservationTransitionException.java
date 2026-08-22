package com.fiap.techchallenge.inventory.exceptions;

import com.fiap.techchallenge.inventory.enums.ReservationStatus;
import lombok.Getter;

@Getter
public class IllegalReservationTransitionException extends RuntimeException {

    private final ReservationStatus from;
    private final ReservationStatus to;

    public IllegalReservationTransitionException(ReservationStatus from, ReservationStatus to) {
        super("Illegal reservation transition: " + from + " -> " + to);

        this.from = from;
        this.to = to;
    }
}
