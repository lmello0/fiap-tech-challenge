package com.fiap.workshop.workorder.application.port.in;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.util.List;
import java.util.UUID;

public interface ProposeAdditionalBudgetUseCase {

	WorkOrder execute(Command command);

	record Command(UUID workOrderId, List<ItemRequest> items, Actor actor) {
	}
}
