package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.application.port.in.CompleteDiagnosisUseCase;
import com.fiap.workshop.workorder.domain.Budget;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CompleteDiagnosisService implements CompleteDiagnosisUseCase {

	private final WorkOrderSupport support;

	CompleteDiagnosisService(WorkOrderSupport support) {
		this.support = support;
	}

	@Override
	@Transactional
	public WorkOrder execute(UUID workOrderId, Actor actor) {
		WorkOrder workOrder = support.load(workOrderId);
		int before = workOrder.history().size();
		Instant now = support.clock().instant();
		Budget budget = workOrder.completeDiagnosis(actor, now);
		WorkOrder saved = support.save(workOrder, before);
		support.publish(WorkOrderEvents.budgetIssued(saved, budget, now));
		return saved;
	}
}
