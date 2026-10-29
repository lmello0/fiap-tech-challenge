package com.fiap.workshop.workorder.application.port.in;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.util.UUID;

public interface CancelWorkOrderUseCase {

	WorkOrder execute(UUID workOrderId, String reason, Actor actor);
}
