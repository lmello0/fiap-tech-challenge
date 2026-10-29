package com.fiap.workshop.catalog.application.port.in;

import com.fiap.workshop.catalog.application.port.out.RepairServiceRepository.ServiceFilter;
import com.fiap.workshop.catalog.domain.RepairService;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;

public interface ListServicesUseCase {

	PageResult<RepairService> execute(ServiceFilter filter, PageQuery page);
}
