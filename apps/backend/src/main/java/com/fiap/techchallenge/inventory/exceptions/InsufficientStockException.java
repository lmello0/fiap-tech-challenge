package com.fiap.techchallenge.inventory.exceptions;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Thrown when a work order tries to start service while one or more of its reservations still carry a
 * shortfall. Reserving is best-effort and never throws this — only consuming is strict.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(UUID workOrderId, Map<UUID, BigDecimal> shortfallsByPartId) {
        super("Work order " + workOrderId + " cannot start: short on " + describe(shortfallsByPartId));
    }

    private static String describe(Map<UUID, BigDecimal> shortfallsByPartId) {
        return shortfallsByPartId.entrySet().stream()
                .map(e -> e.getKey() + " (missing " + e.getValue() + ")")
                .collect(Collectors.joining(", "));
    }
}
