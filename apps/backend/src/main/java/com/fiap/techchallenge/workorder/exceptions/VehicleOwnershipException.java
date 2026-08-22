package com.fiap.techchallenge.workorder.exceptions;

import java.util.UUID;

/** Thrown at work order creation when the given vehicle doesn't exist or doesn't belong to the customer. */
public class VehicleOwnershipException extends RuntimeException {
    public VehicleOwnershipException(UUID vehicleId, UUID customerId) {
        super("Vehicle " + vehicleId + " does not belong to customer " + customerId);
    }
}
