package com.fiap.techchallenge.scheduling.api.representation;

/** The Snapshot (CONTEXT.md) carried by every Appointment Timeline event. */
public record AppointmentSnapshot(
        AppointmentInfo appointment
) {
}
