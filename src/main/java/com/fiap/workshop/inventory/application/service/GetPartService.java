package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.in.GetPartUseCase;
import com.fiap.workshop.inventory.application.port.out.PartRepository;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetPartService implements GetPartUseCase {

	private final PartRepository parts;

	GetPartService(PartRepository parts) {
		this.parts = parts;
	}

	@Override
	@Transactional(readOnly = true)
	public Part execute(UUID id) {
		return parts.findById(id).orElseThrow(() -> new NotFoundException("Part " + id + " not found"));
	}
}
