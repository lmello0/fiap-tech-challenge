package com.fiap.workshop.workorder.application.port.in;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.util.List;

public interface OpenWorkOrderUseCase {

	WorkOrder execute(Command command);

	record Command(String document, String plate, String complaint, List<ItemRequest> items, Actor actor) {
	}
}
