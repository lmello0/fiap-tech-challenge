package com.fiap.techchallenge.scheduling.exceptions;

import com.fiap.techchallenge.scheduling.enums.AppointmentType;

import java.time.Instant;

public class SlotUnavailableException extends RuntimeException {

    public SlotUnavailableException(AppointmentType type, Instant slotStart, String reason) {
        super("Slot unavailable for " + type + " at " + slotStart + ": " + reason);
    }
}
