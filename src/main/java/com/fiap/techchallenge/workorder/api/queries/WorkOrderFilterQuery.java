package com.fiap.techchallenge.workorder.api.queries;

import com.fiap.techchallenge.shared.validators.ValidDateRange;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@ValidDateRange(
        start = "createdAt",
        end = "finishedAt",
        message = "createdAt must be before or equal to finishedAt"
)
public record WorkOrderFilterQuery(
        UUID customerId,
        UUID vehicleId,
        UUID mechanicId,
        WorkOrderStatus status,
        String code,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @PastOrPresent
        LocalDate createdAt,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @PastOrPresent
        LocalDate finishedAt
) {

}
