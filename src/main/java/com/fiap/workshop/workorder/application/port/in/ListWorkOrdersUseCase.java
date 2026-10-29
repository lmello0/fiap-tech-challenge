package com.fiap.workshop.workorder.application.port.in;

import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository.WorkOrderFilter;
import com.fiap.workshop.workorder.application.port.out.WorkOrderSummary;

public interface ListWorkOrdersUseCase {

	PageResult<WorkOrderSummary> execute(WorkOrderFilter filter, PageQuery page);
}
