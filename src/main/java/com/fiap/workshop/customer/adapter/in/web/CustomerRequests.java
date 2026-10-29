package com.fiap.workshop.customer.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

final class CustomerRequests {

	private CustomerRequests() {
	}

	record Create(@NotBlank @Size(max = 150) String name, @NotBlank String document, @NotBlank @Email String email,
			@Size(max = 20) String phone) {
	}

	record Update(@NotBlank @Size(max = 150) String name, @NotBlank @Email String email, @Size(max = 20) String phone) {
	}
}
