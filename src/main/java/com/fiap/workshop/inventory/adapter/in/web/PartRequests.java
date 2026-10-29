package com.fiap.workshop.inventory.adapter.in.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

final class PartRequests {

	private PartRequests() {
	}

	record Create(@NotBlank @Size(max = 40) String sku, @NotBlank @Size(max = 120) String name, @Size(max = 500) String description,
			@NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice, @Min(0) int minimumQuantity) {
	}

	record Update(@NotBlank @Size(max = 120) String name, @Size(max = 500) String description,
			@NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice, @Min(0) int minimumQuantity) {
	}

	record StockEntry(@Min(1) @Max(1_000_000) int quantity, @Size(max = 255) String note) {
	}

	record StockAdjustment(@Min(-1_000_000) @Max(1_000_000) int delta, @NotBlank @Size(max = 255) String reason) {
	}
}
