package com.fiap.techchallenge.scheduling.exceptions;

public class GuestBookingLimitExceededException extends RuntimeException {

    public GuestBookingLimitExceededException() {
        super("This contact already has an active appointment");
    }
}
