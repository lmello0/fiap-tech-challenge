package com.fiap.workshop.customer.adapter.in.web;

import com.fiap.workshop.customer.application.port.in.DeactivateCustomerUseCase;
import com.fiap.workshop.customer.application.port.in.GetCustomerUseCase;
import com.fiap.workshop.customer.application.port.in.ListCustomersUseCase;
import com.fiap.workshop.customer.application.port.in.RegisterCustomerUseCase;
import com.fiap.workshop.customer.application.port.in.UpdateCustomerUseCase;
import com.fiap.workshop.customer.application.port.out.CustomerRepository.CustomerFilter;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.security.Access;
import com.fiap.workshop.shared.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
class CustomerController {

	private final RegisterCustomerUseCase register;
	private final UpdateCustomerUseCase update;
	private final DeactivateCustomerUseCase deactivate;
	private final GetCustomerUseCase get;
	private final ListCustomersUseCase list;

	CustomerController(RegisterCustomerUseCase register, UpdateCustomerUseCase update, DeactivateCustomerUseCase deactivate,
			GetCustomerUseCase get, ListCustomersUseCase list) {
		this.register = register;
		this.update = update;
		this.deactivate = deactivate;
		this.get = get;
		this.list = list;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Access.STAFF_WRITE)
	ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequests.Create request) {
		CustomerResponse body = CustomerResponse.from(
				register.execute(new RegisterCustomerUseCase.Command(request.name(), request.document(), request.email(), request.phone())));
		return ResponseEntity.created(URI.create("/api/v1/customers/" + body.id())).body(body);
	}

	@GetMapping
	@PreAuthorize(Access.STAFF_READ)
	PageResponse<CustomerResponse> search(@RequestParam(required = false) String name,
			@RequestParam(required = false) String document, @RequestParam(defaultValue = "false") boolean includeInactive,
			@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
		return PageResponse.of(list.execute(new CustomerFilter(name, document, includeInactive), new PageQuery(page, size)),
				CustomerResponse::from);
	}

	@GetMapping("/{id}")
	@PreAuthorize(Access.STAFF_READ)
	CustomerResponse find(@PathVariable UUID id) {
		return CustomerResponse.from(get.execute(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize(Access.STAFF_WRITE)
	CustomerResponse edit(@PathVariable UUID id, @Valid @RequestBody CustomerRequests.Update request) {
		return CustomerResponse.from(
				update.execute(new UpdateCustomerUseCase.Command(id, request.name(), request.email(), request.phone())));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(Access.STAFF_WRITE)
	ResponseEntity<Void> remove(@PathVariable UUID id) {
		deactivate.execute(id);
		return ResponseEntity.noContent().build();
	}
}
