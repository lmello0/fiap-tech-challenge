package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.commands.CreatePartCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdatePartCommand;
import com.fiap.techchallenge.inventory.api.queries.PartFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.entities.Part;
import com.fiap.techchallenge.inventory.exceptions.PartNotFoundException;
import com.fiap.techchallenge.inventory.mappers.PartMapper;
import com.fiap.techchallenge.inventory.repositories.PartRepository;
import com.fiap.techchallenge.inventory.repositories.specifications.PartSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartCatalogServiceImpl implements PartCatalogService {

    private final PartRepository partRepository;
    private final PartMapper partMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<PartInfo> listParts(PartFilterQuery filter, Pageable pageable) {
        Specification<Part> spec = Specification
                .where(PartSpecifications.skuEquals(filter.sku()))
                .and(PartSpecifications.nameContains(filter.name()))
                .and(PartSpecifications.brandEquals(filter.brand()))
                .and(PartSpecifications.unitOfMeasureIn(filter.unitOfMeasures()))
                .and(PartSpecifications.activeEquals(filter.active()));

        return partRepository
                .findAll(spec, pageable)
                .map(partMapper::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public PartInfo getById(UUID id) {
        return partRepository
                .findById(id)
                .map(partMapper::toInfo)
                .orElseThrow(() -> new PartNotFoundException(id));
    }

    @Override
    @Transactional
    public PartInfo create(CreatePartCommand command) {
        Part part = new Part();

        part.setSku(command.sku());
        part.setName(command.name());
        part.setDescription(command.description());
        part.setBrand(command.brand());
        part.setUnitOfMeasure(command.unitOfMeasure());
        part.setSalePrice(command.salePrice());

        partRepository.save(part);

        return partMapper.toInfo(part);
    }

    @Override
    @Transactional
    public PartInfo update(UUID id, UpdatePartCommand command) {
        Part part = load(id);

        part.setName(command.name());
        part.setDescription(command.description());
        part.setBrand(command.brand());
        part.setUnitOfMeasure(command.unitOfMeasure());
        part.setSalePrice(command.salePrice());

        return partMapper.toInfo(part);
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        Part part = load(id);

        part.deactivate();
    }

    private Part load(UUID id) {
        return partRepository
                .findById(id)
                .orElseThrow(() -> new PartNotFoundException(id));
    }
}
