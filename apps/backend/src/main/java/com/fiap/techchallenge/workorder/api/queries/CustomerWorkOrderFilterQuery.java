package com.fiap.techchallenge.workorder.api.queries;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;

import java.util.UUID;

/**
 * The only two questions a customer's own work order list needs to answer: "show me what's open" and
 * "show me this car's history". Narrower than the staff {@link WorkOrderFilterQuery} on purpose — a
 * customer has no business filtering by mechanic or by another customer's name.
 */
public record CustomerWorkOrderFilterQuery(
        WorkOrderStatus status,
        UUID vehicleId
) {
}
