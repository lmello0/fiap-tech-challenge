package com.fiap.techchallenge.scheduling.exceptions;

import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;

import java.util.UUID;

public class IllegalAppointmentStateException extends RuntimeException {

    public IllegalAppointmentStateException(UUID appointmentId, AppointmentStatus current, String action) {
        super("Appointment " + appointmentId + " cannot " + action + " while " + current);
    }
}
