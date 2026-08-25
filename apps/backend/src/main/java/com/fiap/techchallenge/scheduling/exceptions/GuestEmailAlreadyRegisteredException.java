package com.fiap.techchallenge.scheduling.exceptions;

public class GuestEmailAlreadyRegisteredException extends RuntimeException {

    public GuestEmailAlreadyRegisteredException() {
        super("This email belongs to a registered account — sign in to book instead");
    }
}
