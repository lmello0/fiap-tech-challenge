package com.fiap.workshop.catalog.application.service;

import com.fiap.workshop.catalog.application.port.in.ListServicesUseCase;
import com.fiap.workshop.catalog.application.port.out.RepairServiceRepository;
import com.fiap.workshop.catalog.application.port.out.RepairServiceRepository.ServiceFilter;
import com.fiap.workshop.catalog.domain.RepairService;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListServicesService implements ListServicesUseCase {

	private final RepairServiceRepository services;

	ListServicesService(RepairServiceRepository services) {
		this.services = services;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<RepairService> execute(ServiceFilter filter, PageQuery page) {
		return services.search(filter, page);
	}
}
