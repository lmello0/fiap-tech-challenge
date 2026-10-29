package com.fiap.workshop.workorder.api.events;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetLineData(String type, UUID referenceId, String name, BigDecimal unitPrice, int quantity) {
}
