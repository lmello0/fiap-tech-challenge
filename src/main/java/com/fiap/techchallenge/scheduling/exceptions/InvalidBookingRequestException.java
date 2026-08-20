package com.fiap.techchallenge.scheduling.exceptions;

/** Neither/both of the two shapes a booking-on-behalf request may take were supplied. */
public class InvalidBookingRequestException extends RuntimeException {

    public InvalidBookingRequestException(String message) {
        super(message);
    }
}
