package com.fiap.workshop.customer.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

final class VehicleRequests {

	private VehicleRequests() {
	}

	@Schema(name = "VehicleCreateRequest")
	record Create(@NotBlank String plate, @NotBlank @Size(max = 60) String brand, @NotBlank @Size(max = 60) String model,
			int year) {
	}

	@Schema(name = "VehicleUpdateRequest")
	record Update(@NotBlank @Size(max = 60) String brand, @NotBlank @Size(max = 60) String model, int year) {
	}
}
