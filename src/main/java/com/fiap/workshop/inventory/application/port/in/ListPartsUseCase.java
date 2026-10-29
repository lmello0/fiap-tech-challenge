package com.fiap.workshop.inventory.application.port.in;

import com.fiap.workshop.inventory.application.port.out.PartRepository.PartFilter;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;

public interface ListPartsUseCase {

	PageResult<Part> execute(PartFilter filter, PageQuery page);
}
