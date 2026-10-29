package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.in.ListPartsUseCase;
import com.fiap.workshop.inventory.application.port.out.PartRepository;
import com.fiap.workshop.inventory.application.port.out.PartRepository.PartFilter;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListPartsService implements ListPartsUseCase {

	private final PartRepository parts;

	ListPartsService(PartRepository parts) {
		this.parts = parts;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<Part> execute(PartFilter filter, PageQuery page) {
		return parts.search(filter, page);
	}
}
