package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.application.port.in.StartDiagnosisUseCase;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class StartDiagnosisService implements StartDiagnosisUseCase {

	private final WorkOrderSupport support;

	StartDiagnosisService(WorkOrderSupport support) {
		this.support = support;
	}

	@Override
	@Transactional
	public WorkOrder execute(UUID workOrderId, Actor actor) {
		WorkOrder workOrder = support.load(workOrderId);
		int before = workOrder.history().size();
		workOrder.startDiagnosis(actor, support.clock().instant());
		return support.save(workOrder, before);
	}
}
