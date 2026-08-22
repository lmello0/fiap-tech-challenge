package com.fiap.techchallenge.scheduling.api.representation;

import com.fiap.techchallenge.scheduling.enums.AppointmentCancelReason;
import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;

import java.time.Instant;
import java.util.UUID;

public record AppointmentInfo(
        UUID id,
        AppointmentType type,
        AppointmentStatus status,
        Instant slotStart,
        Instant slotEnd,
        UUID customerId,
        UUID vehicleId,
        String guestName,
        String guestPhone,
        String guestEmail,
        String guestVehicleMake,
        String guestVehicleModel,
        Integer guestVehicleYear,
        String complaint,
        UUID workOrderId,
        AppointmentCancelReason cancelReason,
        String cancelMessage,
        UUID rescheduledToId,
        Instant checkedInAt,
        Instant createdAt,
        Instant updatedAt
) {
}
