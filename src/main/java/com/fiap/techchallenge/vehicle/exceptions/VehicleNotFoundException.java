package com.fiap.techchallenge.vehicle.exceptions;

import java.util.UUID;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(UUID id) {
        super("Vehicle not found: " + id);
    }
}
