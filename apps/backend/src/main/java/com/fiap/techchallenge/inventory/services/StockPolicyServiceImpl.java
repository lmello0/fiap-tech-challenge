package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.api.StockPolicyService;
import com.fiap.techchallenge.inventory.api.commands.CreateStockPolicyCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateStockPolicyCommand;
import com.fiap.techchallenge.inventory.api.events.StockPolicyCreatedEvent;
import com.fiap.techchallenge.inventory.api.events.StockPolicyDeletedEvent;
import com.fiap.techchallenge.inventory.api.events.StockPolicyUpdatedEvent;
import com.fiap.techchallenge.inventory.api.queries.StockPolicyFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.StockPolicyInfo;
import com.fiap.techchallenge.inventory.entities.Part;
import com.fiap.techchallenge.inventory.entities.StockPolicy;
import com.fiap.techchallenge.inventory.entities.Vendor;
import com.fiap.techchallenge.inventory.exceptions.PartNotFoundException;
import com.fiap.techchallenge.inventory.exceptions.StockPolicyNotFoundException;
import com.fiap.techchallenge.inventory.exceptions.VendorNotFoundException;
import com.fiap.techchallenge.inventory.mappers.StockPolicyMapper;
import com.fiap.techchallenge.inventory.repositories.PartRepository;
import com.fiap.techchallenge.inventory.repositories.StockPolicyRepository;
import com.fiap.techchallenge.inventory.repositories.VendorRepository;
import com.fiap.techchallenge.inventory.repositories.specifications.StockPolicySpecifications;
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
public class StockPolicyServiceImpl implements StockPolicyService {

    private final StockPolicyRepository stockPolicyRepository;
    private final PartRepository partRepository;
    private final VendorRepository vendorRepository;
    private final StockPolicyMapper mapper;
    private final StockPolicyEvaluator evaluator;
    private final ApplicationEventPublisher events;
    private final ActorResolver actorResolver;

    @Override
    @Transactional(readOnly = true)
    public Page<StockPolicyInfo> listStockPolicys(StockPolicyFilterQuery filter, Pageable pageable) {
        Specification<StockPolicy> spec = Specification
                .where(StockPolicySpecifications.partIdEquals(filter.partId()))
                .and(StockPolicySpecifications.vendorIdEquals(filter.vendorId()))
                .and(StockPolicySpecifications.autoReorderEnabledEquals(filter.autoReorderEnabled()));

        return stockPolicyRepository
                .findAll(spec, pageable)
                .map(mapper::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public StockPolicyInfo getById(UUID id) {
        return stockPolicyRepository
                .findById(id)
                .map(mapper::toInfo)
                .orElseThrow(() -> new StockPolicyNotFoundException(id));
    }

    @Override
    @Transactional
    public StockPolicyInfo create(CreateStockPolicyCommand command) {
        Part part = partRepository
                .findById(command.partId())
                .orElseThrow(() -> new PartNotFoundException(command.partId()));

        StockPolicy policy = new StockPolicy();
        policy.setPart(part);
        policy.setMinQuantity(command.minQuantity());
        applyAutoReorderFields(policy, command.autoReorderEnabled(), command.maxQuantity(), command.vendorId());

        stockPolicyRepository.save(policy);

        events.publishEvent(new StockPolicyCreatedEvent(
                part.getId(), policy.getId(), actorResolver.forCurrentUser(false), mapper.toInfo(policy)));

        // A policy can be created for a part that's already below its own minimum; evaluate right
        // away rather than leaving it dormant until the next stock-moving event or the nightly sweep.
        evaluator.evaluate(part.getId());

        return mapper.toInfo(policy);
    }

    @Override
    @Transactional
    public StockPolicyInfo update(UUID id, UpdateStockPolicyCommand command) {
        StockPolicy policy = load(id);

        policy.setMinQuantity(command.minQuantity());
        applyAutoReorderFields(policy, command.autoReorderEnabled(), command.maxQuantity(), command.vendorId());

        events.publishEvent(new StockPolicyUpdatedEvent(
                policy.getPart().getId(), policy.getId(), actorResolver.forCurrentUser(false), mapper.toInfo(policy)));

        evaluator.evaluate(policy.getPart().getId());

        return mapper.toInfo(policy);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        StockPolicy policy = load(id);

        events.publishEvent(new StockPolicyDeletedEvent(
                policy.getPart().getId(), policy.getId(), actorResolver.forCurrentUser(false), mapper.toInfo(policy)));

        stockPolicyRepository.delete(policy);
    }

    /**
     * {@code maxQuantity} and {@code vendor} are only meaningful — and only required — when
     * auto-reorder is on; turning it off leaves the policy as a bare low-stock threshold. A supplied
     * {@code vendorId} is still validated even when disabled, so a bad reference fails fast rather
     * than being silently discarded.
     */
    private void applyAutoReorderFields(StockPolicy policy, boolean autoReorderEnabled, java.math.BigDecimal maxQuantity, UUID vendorId) {
        policy.setAutoReorderEnabled(autoReorderEnabled);

        if (autoReorderEnabled && (maxQuantity == null || vendorId == null)) {
            throw new IllegalArgumentException("Maximum quantity and vendor are required when auto-reorder is enabled");
        }

        Vendor vendor = null;

        if (vendorId != null) {
            vendor = vendorRepository
                    .findById(vendorId)
                    .orElseThrow(() -> new VendorNotFoundException(vendorId));
        }

        policy.setMaxQuantity(autoReorderEnabled ? maxQuantity : null);
        policy.setVendor(autoReorderEnabled ? vendor : null);
    }

    private StockPolicy load(UUID id) {
        return stockPolicyRepository
                .findById(id)
                .orElseThrow(() -> new StockPolicyNotFoundException(id));
    }
}
