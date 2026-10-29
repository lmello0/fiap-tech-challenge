package com.fiap.workshop.workorder.application.port.in;

import com.fiap.workshop.workorder.domain.ItemType;
import java.util.UUID;

public record ItemRequest(ItemType type, UUID referenceId, int quantity) {
}
