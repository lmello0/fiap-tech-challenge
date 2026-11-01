package com.fiap.workshop.catalog.adapter.in.web;

import com.fiap.workshop.catalog.application.port.in.CreateServiceUseCase;
import com.fiap.workshop.catalog.application.port.in.DeactivateServiceUseCase;
import com.fiap.workshop.catalog.application.port.in.GetServiceUseCase;
import com.fiap.workshop.catalog.application.port.in.ListServicesUseCase;
import com.fiap.workshop.catalog.application.port.in.UpdateServiceUseCase;
import com.fiap.workshop.catalog.application.port.out.RepairServiceRepository.ServiceFilter;
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
@RequestMapping("/api/v1/services")
class ServiceController {

	private final CreateServiceUseCase create;
	private final UpdateServiceUseCase update;
	private final DeactivateServiceUseCase deactivate;
	private final GetServiceUseCase get;
	private final ListServicesUseCase list;

	ServiceController(CreateServiceUseCase create, UpdateServiceUseCase update, DeactivateServiceUseCase deactivate,
			GetServiceUseCase get, ListServicesUseCase list) {
		this.create = create;
		this.update = update;
		this.deactivate = deactivate;
		this.get = get;
		this.list = list;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Access.STAFF_WRITE)
	ResponseEntity<ServiceResponse> add(@Valid @RequestBody ServiceRequest request) {
		ServiceResponse body = ServiceResponse.from(create.execute(
				new CreateServiceUseCase.Command(request.name(), request.description(), request.price(), request.estimatedMinutes())));
		return ResponseEntity.created(URI.create("/api/v1/services/" + body.id())).body(body);
	}

	@GetMapping
	@PreAuthorize(Access.STAFF_READ)
	PageResponse<ServiceResponse> search(@RequestParam(required = false) String name,
			@RequestParam(defaultValue = "false") boolean includeInactive, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
		return PageResponse.of(list.execute(new ServiceFilter(name, includeInactive), new PageQuery(page, size)),
				ServiceResponse::from);
	}

	@GetMapping("/{id}")
	@PreAuthorize(Access.STAFF_READ)
	ServiceResponse find(@PathVariable UUID id) {
		return ServiceResponse.from(get.execute(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize(Access.STAFF_WRITE)
	ServiceResponse edit(@PathVariable UUID id, @Valid @RequestBody ServiceRequest request) {
		return ServiceResponse.from(update.execute(new UpdateServiceUseCase.Command(id, request.name(), request.description(),
				request.price(), request.estimatedMinutes())));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(Access.STAFF_WRITE)
	ResponseEntity<Void> remove(@PathVariable UUID id) {
		deactivate.execute(id);
		return ResponseEntity.noContent().build();
	}
}
