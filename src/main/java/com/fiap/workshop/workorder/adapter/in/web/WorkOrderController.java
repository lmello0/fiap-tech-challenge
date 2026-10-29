package com.fiap.workshop.workorder.adapter.in.web;

import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.security.Access;
import com.fiap.workshop.shared.security.CurrentActor;
import com.fiap.workshop.shared.web.PageResponse;
import com.fiap.workshop.workorder.application.port.in.AddItemUseCase;
import com.fiap.workshop.workorder.application.port.in.ApproveBudgetUseCase;
import com.fiap.workshop.workorder.application.port.in.CancelWorkOrderUseCase;
import com.fiap.workshop.workorder.application.port.in.ChangeItemQuantityUseCase;
import com.fiap.workshop.workorder.application.port.in.CompleteDiagnosisUseCase;
import com.fiap.workshop.workorder.application.port.in.CompleteWorkOrderUseCase;
import com.fiap.workshop.workorder.application.port.in.DeliverWorkOrderUseCase;
import com.fiap.workshop.workorder.application.port.in.GetWorkOrderMetricsUseCase;
import com.fiap.workshop.workorder.application.port.in.GetWorkOrderUseCase;
import com.fiap.workshop.workorder.application.port.in.ListWorkOrdersUseCase;
import com.fiap.workshop.workorder.application.port.in.OpenWorkOrderUseCase;
import com.fiap.workshop.workorder.application.port.in.ProposeAdditionalBudgetUseCase;
import com.fiap.workshop.workorder.application.port.in.RejectBudgetUseCase;
import com.fiap.workshop.workorder.application.port.in.RemoveItemUseCase;
import com.fiap.workshop.workorder.application.port.in.StartDiagnosisUseCase;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository.WorkOrderFilter;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/work-orders")
class WorkOrderController {

	private final OpenWorkOrderUseCase open;
	private final GetWorkOrderUseCase get;
	private final ListWorkOrdersUseCase list;
	private final AddItemUseCase addItem;
	private final ChangeItemQuantityUseCase changeQuantity;
	private final RemoveItemUseCase removeItem;
	private final StartDiagnosisUseCase startDiagnosis;
	private final CompleteDiagnosisUseCase completeDiagnosis;
	private final ProposeAdditionalBudgetUseCase additionalBudget;
	private final ApproveBudgetUseCase approve;
	private final RejectBudgetUseCase reject;
	private final CompleteWorkOrderUseCase complete;
	private final DeliverWorkOrderUseCase deliver;
	private final CancelWorkOrderUseCase cancel;
	private final GetWorkOrderMetricsUseCase metrics;
	private final CurrentActor currentActor;

	WorkOrderController(OpenWorkOrderUseCase open, GetWorkOrderUseCase get, ListWorkOrdersUseCase list, AddItemUseCase addItem,
			ChangeItemQuantityUseCase changeQuantity, RemoveItemUseCase removeItem, StartDiagnosisUseCase startDiagnosis,
			CompleteDiagnosisUseCase completeDiagnosis, ProposeAdditionalBudgetUseCase additionalBudget, ApproveBudgetUseCase approve,
			RejectBudgetUseCase reject, CompleteWorkOrderUseCase complete, DeliverWorkOrderUseCase deliver,
			CancelWorkOrderUseCase cancel, GetWorkOrderMetricsUseCase metrics, CurrentActor currentActor) {
		this.open = open;
		this.get = get;
		this.list = list;
		this.addItem = addItem;
		this.changeQuantity = changeQuantity;
		this.removeItem = removeItem;
		this.startDiagnosis = startDiagnosis;
		this.completeDiagnosis = completeDiagnosis;
		this.additionalBudget = additionalBudget;
		this.approve = approve;
		this.reject = reject;
		this.complete = complete;
		this.deliver = deliver;
		this.cancel = cancel;
		this.metrics = metrics;
		this.currentActor = currentActor;
	}

	@PostMapping
	@PreAuthorize(Access.STAFF_WRITE)
	ResponseEntity<WorkOrderResponse> openWorkOrder(@Valid @RequestBody WorkOrderRequests.Open request) {
		WorkOrderResponse body = WorkOrderResponse.forStaff(open.execute(new OpenWorkOrderUseCase.Command(request.document(),
				request.plate(), request.complaint(), request.itemRequests(), currentActor.actor())));
		return ResponseEntity.created(URI.create("/api/v1/work-orders/" + body.id())).body(body);
	}

	@GetMapping
	@PreAuthorize(Access.STAFF_READ)
	PageResponse<WorkOrderSummaryResponse> search(@RequestParam(required = false) UUID customerId,
			@RequestParam(required = false) WorkOrderStatus status, @RequestParam(required = false) String number,
			@RequestParam(required = false) String plate, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
		return PageResponse.of(list.execute(new WorkOrderFilter(customerId, status, number, plate), new PageQuery(page, size)),
				WorkOrderSummaryResponse::from);
	}

	@GetMapping("/metrics")
	@PreAuthorize(Access.STAFF_READ)
	WorkOrderMetricsResponse metrics(@RequestParam(required = false) java.time.Instant from,
			@RequestParam(required = false) java.time.Instant to) {
		return WorkOrderMetricsResponse.from(metrics.execute(from, to));
	}

	@GetMapping("/{id}")
	@PreAuthorize(Access.STAFF_READ)
	WorkOrderResponse find(@PathVariable UUID id) {
		return WorkOrderResponse.forStaff(get.execute(id, null));
	}

	@PostMapping("/{id}/items")
	@PreAuthorize(Access.STAFF_READ)
	WorkOrderResponse addItem(@PathVariable UUID id, @Valid @RequestBody WorkOrderRequests.Item request) {
		return WorkOrderResponse.forStaff(addItem.execute(new AddItemUseCase.Command(id, request.toRequest())));
	}

	@PutMapping("/{id}/items/{itemId}")
	@PreAuthorize(Access.STAFF_READ)
	WorkOrderResponse changeQuantity(@PathVariable UUID id, @PathVariable UUID itemId,
			@Valid @RequestBody WorkOrderRequests.Quantity request) {
		return WorkOrderResponse.forStaff(changeQuantity.execute(new ChangeItemQuantityUseCase.Command(id, itemId, request.quantity())));
	}

	@DeleteMapping("/{id}/items/{itemId}")
	@PreAuthorize(Access.STAFF_READ)
	WorkOrderResponse removeItem(@PathVariable UUID id, @PathVariable UUID itemId) {
		return WorkOrderResponse.forStaff(removeItem.execute(id, itemId));
	}

	@PostMapping("/{id}/diagnosis/start")
	@PreAuthorize(Access.MECHANIC_WORK)
	WorkOrderResponse startDiagnosis(@PathVariable UUID id) {
		return WorkOrderResponse.forStaff(startDiagnosis.execute(id, currentActor.actor()));
	}

	@PostMapping("/{id}/diagnosis/complete")
	@PreAuthorize(Access.MECHANIC_WORK)
	WorkOrderResponse completeDiagnosis(@PathVariable UUID id) {
		return WorkOrderResponse.forStaff(completeDiagnosis.execute(id, currentActor.actor()));
	}

	@PostMapping("/{id}/additional-budget")
	@PreAuthorize(Access.MECHANIC_WORK)
	WorkOrderResponse additionalBudget(@PathVariable UUID id, @Valid @RequestBody WorkOrderRequests.Additional request) {
		return WorkOrderResponse.forStaff(additionalBudget.execute(new ProposeAdditionalBudgetUseCase.Command(id,
				request.items().stream().map(WorkOrderRequests.Item::toRequest).toList(), currentActor.actor())));
	}

	@PostMapping("/{id}/budgets/{budgetId}/approve")
	@PreAuthorize(Access.STAFF_WRITE)
	WorkOrderResponse approve(@PathVariable UUID id, @PathVariable UUID budgetId,
			@Valid @RequestBody WorkOrderRequests.StaffApproval request) {
		return WorkOrderResponse.forStaff(
				approve.execute(new ApproveBudgetUseCase.Command(id, budgetId, currentActor.actor(), request.channel(), null)));
	}

	@PostMapping("/{id}/budgets/{budgetId}/reject")
	@PreAuthorize(Access.STAFF_WRITE)
	WorkOrderResponse reject(@PathVariable UUID id, @PathVariable UUID budgetId,
			@Valid @RequestBody WorkOrderRequests.StaffRejection request) {
		return WorkOrderResponse.forStaff(reject.execute(
				new RejectBudgetUseCase.Command(id, budgetId, currentActor.actor(), request.channel(), request.reason(), null)));
	}

	@PostMapping("/{id}/complete")
	@PreAuthorize(Access.MECHANIC_WORK)
	WorkOrderResponse complete(@PathVariable UUID id) {
		return WorkOrderResponse.forStaff(complete.execute(id, currentActor.actor()));
	}

	@PostMapping("/{id}/deliver")
	@PreAuthorize(Access.STAFF_WRITE)
	WorkOrderResponse deliver(@PathVariable UUID id) {
		return WorkOrderResponse.forStaff(deliver.execute(id, currentActor.actor()));
	}

	@PostMapping("/{id}/cancel")
	@PreAuthorize(Access.STAFF_WRITE)
	WorkOrderResponse cancel(@PathVariable UUID id, @Valid @RequestBody(required = false) WorkOrderRequests.Cancellation request) {
		return WorkOrderResponse.forStaff(cancel.execute(id, request == null ? null : request.reason(), currentActor.actor()));
	}
}
