package com.fiap.workshop.workorder.adapter.in.web;

import com.fiap.workshop.workorder.application.port.in.ItemRequest;
import com.fiap.workshop.workorder.domain.DecisionChannel;
import com.fiap.workshop.workorder.domain.ItemType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

final class WorkOrderRequests {

	private WorkOrderRequests() {
	}

	record Item(@NotNull ItemType type, @NotNull UUID referenceId, @Min(1) @Max(999) int quantity) {

		ItemRequest toRequest() {
			return new ItemRequest(type, referenceId, quantity);
		}
	}

	record Open(@NotBlank String document, @NotBlank String plate, @Size(max = 1000) String complaint, @Valid List<Item> items) {

		List<ItemRequest> itemRequests() {
			return items == null ? List.of() : items.stream().map(Item::toRequest).toList();
		}
	}

	record Quantity(@Min(1) @Max(999) int quantity) {
	}

	record Additional(@NotEmpty @Valid List<Item> items) {
	}

	record StaffApproval(@NotNull DecisionChannel channel) {
	}

	record StaffRejection(@NotNull DecisionChannel channel, @Size(max = 500) String reason) {
	}

	record CustomerRejection(@Size(max = 500) String reason) {
	}

	record Cancellation(@Size(max = 500) String reason) {
	}
}
