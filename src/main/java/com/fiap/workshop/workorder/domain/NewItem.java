package com.fiap.workshop.workorder.domain;

import com.fiap.workshop.shared.domain.Money;
import java.util.UUID;

public record NewItem(ItemType type, UUID referenceId, String name, Money unitPrice, int quantity) {
}
