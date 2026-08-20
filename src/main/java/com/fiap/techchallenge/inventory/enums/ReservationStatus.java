package com.fiap.techchallenge.inventory.enums;

public enum ReservationStatus {
    /** Holding a claim on stock; may carry a shortfall if less than requested was available. */
    HELD,
    /** The work order started service; the reservation's stock was written off via a CONSUMPTION movement. */
    CONSUMED,
    /** The work order was refused; any stock the reservation held was returned to availability. */
    RELEASED,
    /** Aged past the reservation TTL with the work order never starting service; stock was returned. */
    EXPIRED
}
