package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.api.ReorderRuleService;
import com.fiap.techchallenge.inventory.api.commands.CreateReorderRuleCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateReorderRuleCommand;
import com.fiap.techchallenge.inventory.api.queries.ReorderRuleFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.ReorderRuleInfo;
import com.fiap.techchallenge.inventory.entities.Part;
import com.fiap.techchallenge.inventory.entities.ReorderRule;
import com.fiap.techchallenge.inventory.entities.Vendor;
import com.fiap.techchallenge.inventory.exceptions.PartNotFoundException;
import com.fiap.techchallenge.inventory.exceptions.ReorderRuleNotFoundException;
import com.fiap.techchallenge.inventory.exceptions.VendorNotFoundException;
import com.fiap.techchallenge.inventory.mappers.ReorderRuleMapper;
import com.fiap.techchallenge.inventory.repositories.PartRepository;
import com.fiap.techchallenge.inventory.repositories.ReorderRuleRepository;
import com.fiap.techchallenge.inventory.repositories.VendorRepository;
import com.fiap.techchallenge.inventory.repositories.specifications.ReorderRuleSpecifications;
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
public class ReorderRuleServiceImpl implements ReorderRuleService {

    private final ReorderRuleRepository reorderRuleRepository;
    private final PartRepository partRepository;
    private final VendorRepository vendorRepository;
    private final ReorderRuleMapper mapper;
    private final ReorderRuleEvaluator evaluator;

    @Override
    @Transactional(readOnly = true)
    public Page<ReorderRuleInfo> listReorderRules(ReorderRuleFilterQuery filter, Pageable pageable) {
        Specification<ReorderRule> spec = Specification
                .where(ReorderRuleSpecifications.partIdEquals(filter.partId()))
                .and(ReorderRuleSpecifications.vendorIdEquals(filter.vendorId()))
                .and(ReorderRuleSpecifications.enabledEquals(filter.enabled()));

        return reorderRuleRepository
                .findAll(spec, pageable)
                .map(mapper::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public ReorderRuleInfo getById(UUID id) {
        return reorderRuleRepository
                .findById(id)
                .map(mapper::toInfo)
                .orElseThrow(() -> new ReorderRuleNotFoundException(id));
    }

    @Override
    @Transactional
    public ReorderRuleInfo create(CreateReorderRuleCommand command) {
        Part part = partRepository
                .findById(command.partId())
                .orElseThrow(() -> new PartNotFoundException(command.partId()));

        Vendor vendor = vendorRepository
                .findById(command.vendorId())
                .orElseThrow(() -> new VendorNotFoundException(command.vendorId()));

        ReorderRule rule = new ReorderRule();
        rule.setPart(part);
        rule.setMinQuantity(command.minQuantity());
        rule.setMaxQuantity(command.maxQuantity());
        rule.setVendor(vendor);
        rule.setEnabled(command.enabled());

        reorderRuleRepository.save(rule);

        // A rule can be created for a part that's already below its own minimum; evaluate right away
        // rather than leaving it dormant until the next stock-moving event or the nightly sweep.
        evaluator.evaluate(part.getId());

        return mapper.toInfo(rule);
    }

    @Override
    @Transactional
    public ReorderRuleInfo update(UUID id, UpdateReorderRuleCommand command) {
        ReorderRule rule = load(id);

        Vendor vendor = vendorRepository
                .findById(command.vendorId())
                .orElseThrow(() -> new VendorNotFoundException(command.vendorId()));

        rule.setMinQuantity(command.minQuantity());
        rule.setMaxQuantity(command.maxQuantity());
        rule.setVendor(vendor);
        rule.setEnabled(command.enabled());

        evaluator.evaluate(rule.getPart().getId());

        return mapper.toInfo(rule);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ReorderRule rule = load(id);

        reorderRuleRepository.delete(rule);
    }

    private ReorderRule load(UUID id) {
        return reorderRuleRepository
                .findById(id)
                .orElseThrow(() -> new ReorderRuleNotFoundException(id));
    }
}
