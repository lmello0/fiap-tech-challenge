package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.api.VendorService;
import com.fiap.techchallenge.inventory.api.commands.CreateVendorCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateVendorCommand;
import com.fiap.techchallenge.inventory.api.events.VendorCreatedEvent;
import com.fiap.techchallenge.inventory.api.events.VendorDeactivatedEvent;
import com.fiap.techchallenge.inventory.api.events.VendorUpdatedEvent;
import com.fiap.techchallenge.inventory.api.queries.VendorFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.VendorInfo;
import com.fiap.techchallenge.inventory.entities.Vendor;
import com.fiap.techchallenge.inventory.exceptions.VendorNotFoundException;
import com.fiap.techchallenge.inventory.mappers.VendorMapper;
import com.fiap.techchallenge.inventory.repositories.VendorRepository;
import com.fiap.techchallenge.inventory.repositories.specifications.VendorSpecifications;
import com.fiap.techchallenge.shared.audit.ActorResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;
    private final ApplicationEventPublisher events;
    private final ActorResolver actorResolver;

    @Override
    @Transactional(readOnly = true)
    public Page<VendorInfo> listVendors(VendorFilterQuery filter, Pageable pageable) {
        Specification<Vendor> spec = Specification
                .where(VendorSpecifications.nameContains(filter.name()))
                .and(VendorSpecifications.activeEquals(filter.active()));

        return vendorRepository
                .findAll(spec, pageable)
                .map(vendorMapper::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorInfo getById(UUID id) {
        return vendorRepository
                .findById(id)
                .map(vendorMapper::toInfo)
                .orElseThrow(() -> new VendorNotFoundException(id));
    }

    @Override
    @Transactional
    public VendorInfo create(CreateVendorCommand command) {
        Vendor vendor = new Vendor();

        vendor.setName(command.name());
        vendor.setContactEmail(command.contactEmail());

        vendorRepository.save(vendor);

        events.publishEvent(new VendorCreatedEvent(vendor.getId(), actorResolver.forCurrentUser(false), vendorMapper.toInfo(vendor)));

        return vendorMapper.toInfo(vendor);
    }

    @Override
    @Transactional
    public VendorInfo update(UUID id, UpdateVendorCommand command) {
        Vendor vendor = load(id);

        vendor.setName(command.name());
        vendor.setContactEmail(command.contactEmail());

        events.publishEvent(new VendorUpdatedEvent(vendor.getId(), actorResolver.forCurrentUser(false), vendorMapper.toInfo(vendor)));

        return vendorMapper.toInfo(vendor);
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        Vendor vendor = load(id);

        vendor.deactivate();

        events.publishEvent(new VendorDeactivatedEvent(vendor.getId(), actorResolver.forCurrentUser(false), vendorMapper.toInfo(vendor)));
    }

    private Vendor load(UUID id) {
        return vendorRepository
                .findById(id)
                .orElseThrow(() -> new VendorNotFoundException(id));
    }
}
