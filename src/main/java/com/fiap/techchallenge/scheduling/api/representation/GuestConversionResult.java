package com.fiap.techchallenge.scheduling.api.representation;

/**
 * {@code temporaryPassword} is only populated for the Attendant-initiated Check-in path — nobody
 * else knows it, since the customer never chose it, so it must be handed back to the caller to
 * relay. Null for self-service Guest Conversion, where the customer chose their own password.
 */
public record GuestConversionResult(
        AppointmentInfo appointment,
        String temporaryPassword
) {
}
