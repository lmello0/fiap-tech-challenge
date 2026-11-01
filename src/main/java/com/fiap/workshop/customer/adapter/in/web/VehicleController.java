package com.fiap.workshop.customer.adapter.in.web;

import com.fiap.workshop.customer.application.port.in.DeactivateVehicleUseCase;
import com.fiap.workshop.customer.application.port.in.GetVehicleUseCase;
import com.fiap.workshop.customer.application.port.in.ListVehiclesUseCase;
import com.fiap.workshop.customer.application.port.in.RegisterVehicleUseCase;
import com.fiap.workshop.customer.application.port.in.UpdateVehicleUseCase;
import com.fiap.workshop.customer.application.port.out.VehicleRepository.VehicleFilter;
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
@RequestMapping("/api/v1")
class VehicleController {

	private final RegisterVehicleUseCase register;
	private final UpdateVehicleUseCase update;
	private final DeactivateVehicleUseCase deactivate;
	private final GetVehicleUseCase get;
	private final ListVehiclesUseCase list;

	VehicleController(RegisterVehicleUseCase register, UpdateVehicleUseCase update, DeactivateVehicleUseCase deactivate,
			GetVehicleUseCase get, ListVehiclesUseCase list) {
		this.register = register;
		this.update = update;
		this.deactivate = deactivate;
		this.get = get;
		this.list = list;
	}

	@PostMapping("/customers/{customerId}/vehicles")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Access.STAFF_WRITE)
	ResponseEntity<VehicleResponse> create(@PathVariable UUID customerId, @Valid @RequestBody VehicleRequests.Create request) {
		VehicleResponse body = VehicleResponse.from(register.execute(
				new RegisterVehicleUseCase.Command(customerId, request.plate(), request.brand(), request.model(), request.year())));
		return ResponseEntity.created(URI.create("/api/v1/vehicles/" + body.id())).body(body);
	}

	@GetMapping("/customers/{customerId}/vehicles")
	@PreAuthorize(Access.STAFF_READ)
	PageResponse<VehicleResponse> ofCustomer(@PathVariable UUID customerId,
			@RequestParam(defaultValue = "false") boolean includeInactive, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
		return PageResponse.of(list.execute(new VehicleFilter(customerId, includeInactive), new PageQuery(page, size)),
				VehicleResponse::from);
	}

	@GetMapping("/vehicles")
	@PreAuthorize(Access.STAFF_READ)
	PageResponse<VehicleResponse> search(@RequestParam(defaultValue = "false") boolean includeInactive,
			@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
		return PageResponse.of(list.execute(new VehicleFilter(null, includeInactive), new PageQuery(page, size)),
				VehicleResponse::from);
	}

	@GetMapping("/vehicles/{id}")
	@PreAuthorize(Access.STAFF_READ)
	VehicleResponse find(@PathVariable UUID id) {
		return VehicleResponse.from(get.execute(id));
	}

	@PutMapping("/vehicles/{id}")
	@PreAuthorize(Access.STAFF_WRITE)
	VehicleResponse edit(@PathVariable UUID id, @Valid @RequestBody VehicleRequests.Update request) {
		return VehicleResponse.from(
				update.execute(new UpdateVehicleUseCase.Command(id, request.brand(), request.model(), request.year())));
	}

	@DeleteMapping("/vehicles/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(Access.STAFF_WRITE)
	ResponseEntity<Void> remove(@PathVariable UUID id) {
		deactivate.execute(id);
		return ResponseEntity.noContent().build();
	}
}
