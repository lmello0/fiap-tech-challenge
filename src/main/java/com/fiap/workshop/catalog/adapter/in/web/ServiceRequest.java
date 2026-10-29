package com.fiap.workshop.catalog.adapter.in.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

record ServiceRequest(@NotBlank @Size(max = 120) String name, @Size(max = 500) String description,
		@NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price, @Min(1) int estimatedMinutes) {
}
