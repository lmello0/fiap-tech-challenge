package com.fiap.workshop.workorder.application.port.in;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.domain.DecisionChannel;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.util.UUID;

public interface ApproveBudgetUseCase {

	WorkOrder execute(Command command);

	record Command(UUID workOrderId, UUID budgetId, Actor actor, DecisionChannel channel, UUID customerScope) {
	}
}
