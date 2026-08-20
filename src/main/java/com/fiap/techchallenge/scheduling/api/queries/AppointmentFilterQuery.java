package com.fiap.techchallenge.scheduling.api.queries;

import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

public record AppointmentFilterQuery(
        AppointmentType type,
        AppointmentStatus status,
        UUID customerId,
        UUID workOrderId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date
) {
}
