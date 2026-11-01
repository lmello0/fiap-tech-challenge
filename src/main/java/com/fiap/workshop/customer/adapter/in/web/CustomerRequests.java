package com.fiap.workshop.customer.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

final class CustomerRequests {

	private CustomerRequests() {
	}

	@Schema(name = "CustomerCreateRequest")
	record Create(@NotBlank @Size(max = 150) String name, @NotBlank String document, @NotBlank @Email String email,
			@Size(max = 20) String phone) {
	}

	@Schema(name = "CustomerUpdateRequest")
	record Update(@NotBlank @Size(max = 150) String name, @NotBlank @Email String email, @Size(max = 20) String phone) {
	}
}
