package com.fiap.workshop.customer.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

final class VehicleRequests {

	private VehicleRequests() {
	}

	record Create(@NotBlank String plate, @NotBlank @Size(max = 60) String brand, @NotBlank @Size(max = 60) String model,
			int year) {
	}

	record Update(@NotBlank @Size(max = 60) String brand, @NotBlank @Size(max = 60) String model, int year) {
	}
}
