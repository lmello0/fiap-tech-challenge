package com.fiap.techchallenge.user.api.representation;

import com.fiap.techchallenge.user.enums.WorkerRole;

import java.util.UUID;

public record UserPrincipal(
        UUID id,
        String email,
        boolean emailVerified,
        boolean customer,
        boolean customerActive,
        boolean worker,
        boolean workerActive,
        WorkerRole workerRole
) {
    public boolean enabled() {
        return (customer && customerActive) || (worker && workerActive);
    }
}
