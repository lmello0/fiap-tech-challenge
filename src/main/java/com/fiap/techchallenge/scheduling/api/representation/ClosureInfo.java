package com.fiap.techchallenge.scheduling.api.representation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClosureInfo(
        UUID id,
        LocalDate date,
        String message,
        Instant createdAt
) {
}
