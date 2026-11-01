package com.fiap.workshop.inventory.adapter.in.web;

import com.fiap.workshop.inventory.application.port.in.AdjustStockUseCase;
import com.fiap.workshop.inventory.application.port.in.CreatePartUseCase;
import com.fiap.workshop.inventory.application.port.in.DeactivatePartUseCase;
import com.fiap.workshop.inventory.application.port.in.GetPartUseCase;
import com.fiap.workshop.inventory.application.port.in.ListPartsUseCase;
import com.fiap.workshop.inventory.application.port.in.ListStockMovementsUseCase;
import com.fiap.workshop.inventory.application.port.in.RegisterStockEntryUseCase;
import com.fiap.workshop.inventory.application.port.in.UpdatePartUseCase;
import com.fiap.workshop.inventory.application.port.out.PartRepository.PartFilter;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.security.Access;
import com.fiap.workshop.shared.security.CurrentActor;
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
@RequestMapping("/api/v1/parts")
class PartController {

	private final CreatePartUseCase create;
	private final UpdatePartUseCase update;
	private final DeactivatePartUseCase deactivate;
	private final GetPartUseCase get;
	private final ListPartsUseCase list;
	private final RegisterStockEntryUseCase stockEntry;
	private final AdjustStockUseCase adjustStock;
	private final ListStockMovementsUseCase listMovements;
	private final CurrentActor currentActor;

	PartController(CreatePartUseCase create, UpdatePartUseCase update, DeactivatePartUseCase deactivate, GetPartUseCase get,
			ListPartsUseCase list, RegisterStockEntryUseCase stockEntry, AdjustStockUseCase adjustStock,
			ListStockMovementsUseCase listMovements, CurrentActor currentActor) {
		this.create = create;
		this.update = update;
		this.deactivate = deactivate;
		this.get = get;
		this.list = list;
		this.stockEntry = stockEntry;
		this.adjustStock = adjustStock;
		this.listMovements = listMovements;
		this.currentActor = currentActor;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Access.STAFF_WRITE)
	ResponseEntity<PartResponse> add(@Valid @RequestBody PartRequests.Create request) {
		PartResponse body = PartResponse.from(create.execute(new CreatePartUseCase.Command(request.sku(), request.name(),
				request.description(), request.unitPrice(), request.minimumQuantity())));
		return ResponseEntity.created(URI.create("/api/v1/parts/" + body.id())).body(body);
	}

	@GetMapping
	@PreAuthorize(Access.STAFF_READ)
	PageResponse<PartResponse> search(@RequestParam(required = false) String text,
			@RequestParam(defaultValue = "false") boolean lowStockOnly, @RequestParam(defaultValue = "false") boolean includeInactive,
			@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
		return PageResponse.of(list.execute(new PartFilter(text, lowStockOnly, includeInactive), new PageQuery(page, size)),
				PartResponse::from);
	}

	@GetMapping("/{id}")
	@PreAuthorize(Access.STAFF_READ)
	PartResponse find(@PathVariable UUID id) {
		return PartResponse.from(get.execute(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize(Access.STAFF_WRITE)
	PartResponse edit(@PathVariable UUID id, @Valid @RequestBody PartRequests.Update request) {
		return PartResponse.from(update.execute(new UpdatePartUseCase.Command(id, request.name(), request.description(),
				request.unitPrice(), request.minimumQuantity())));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(Access.STAFF_WRITE)
	ResponseEntity<Void> remove(@PathVariable UUID id) {
		deactivate.execute(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/stock-entries")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Access.STAFF_WRITE)
	ResponseEntity<StockMovementResponse> receive(@PathVariable UUID id, @Valid @RequestBody PartRequests.StockEntry request) {
		var movement = stockEntry.execute(
				new RegisterStockEntryUseCase.Command(id, request.quantity(), request.note(), currentActor.get().subject()));
		return ResponseEntity.status(201).body(StockMovementResponse.from(movement));
	}

	@PostMapping("/{id}/stock-adjustments")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Access.STAFF_WRITE)
	ResponseEntity<StockMovementResponse> adjust(@PathVariable UUID id, @Valid @RequestBody PartRequests.StockAdjustment request) {
		var movement = adjustStock.execute(
				new AdjustStockUseCase.Command(id, request.delta(), request.reason(), currentActor.get().subject()));
		return ResponseEntity.status(201).body(StockMovementResponse.from(movement));
	}

	@GetMapping("/{id}/movements")
	@PreAuthorize(Access.STAFF_READ)
	PageResponse<StockMovementResponse> movements(@PathVariable UUID id, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
		return PageResponse.of(listMovements.execute(id, new PageQuery(page, size)), StockMovementResponse::from);
	}
}
