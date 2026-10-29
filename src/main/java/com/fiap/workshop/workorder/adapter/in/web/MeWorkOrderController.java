package com.fiap.workshop.workorder.adapter.in.web;

import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.domain.NotFoundException;
import com.fiap.workshop.shared.security.Access;
import com.fiap.workshop.shared.security.CurrentActor;
import com.fiap.workshop.shared.web.PageResponse;
import com.fiap.workshop.workorder.application.port.in.ApproveBudgetUseCase;
import com.fiap.workshop.workorder.application.port.in.GetWorkOrderUseCase;
import com.fiap.workshop.workorder.application.port.in.ListWorkOrdersUseCase;
import com.fiap.workshop.workorder.application.port.in.RejectBudgetUseCase;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository.WorkOrderFilter;
import com.fiap.workshop.workorder.domain.DecisionChannel;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/work-orders")
@PreAuthorize(Access.CUSTOMER)
class MeWorkOrderController {

	private final ListWorkOrdersUseCase list;
	private final GetWorkOrderUseCase get;
	private final ApproveBudgetUseCase approve;
	private final RejectBudgetUseCase reject;
	private final CurrentActor currentActor;

	MeWorkOrderController(ListWorkOrdersUseCase list, GetWorkOrderUseCase get, ApproveBudgetUseCase approve, RejectBudgetUseCase reject,
			CurrentActor currentActor) {
		this.list = list;
		this.get = get;
		this.approve = approve;
		this.reject = reject;
		this.currentActor = currentActor;
	}

	@GetMapping
	PageResponse<WorkOrderSummaryResponse> mine(@RequestParam(required = false) WorkOrderStatus status,
			@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
		return PageResponse.of(list.execute(new WorkOrderFilter(customerId(), status, null, null), new PageQuery(page, size)),
				WorkOrderSummaryResponse::from);
	}

	@GetMapping("/{id}")
	WorkOrderResponse find(@PathVariable UUID id) {
		return WorkOrderResponse.forCustomer(get.execute(id, customerId()));
	}

	@PostMapping("/{id}/budgets/{budgetId}/approve")
	WorkOrderResponse approve(@PathVariable UUID id, @PathVariable UUID budgetId) {
		return WorkOrderResponse.forCustomer(approve.execute(
				new ApproveBudgetUseCase.Command(id, budgetId, currentActor.actor(), DecisionChannel.APP, customerId())));
	}

	@PostMapping("/{id}/budgets/{budgetId}/reject")
	WorkOrderResponse reject(@PathVariable UUID id, @PathVariable UUID budgetId,
			@Valid @RequestBody(required = false) WorkOrderRequests.CustomerRejection request) {
		return WorkOrderResponse.forCustomer(reject.execute(new RejectBudgetUseCase.Command(id, budgetId, currentActor.actor(),
				DecisionChannel.APP, request == null ? null : request.reason(), customerId())));
	}

	private UUID customerId() {
		UUID customerId = currentActor.get().customerId();
		if (customerId == null) {
			throw new NotFoundException("No customer is linked to this account");
		}
		return customerId;
	}
}
