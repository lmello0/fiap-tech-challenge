package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.api.StockService;
import com.fiap.techchallenge.inventory.api.commands.AdjustStockCommand;
import com.fiap.techchallenge.inventory.api.events.PartPositionMayHaveDroppedEvent;
import com.fiap.techchallenge.inventory.api.queries.PartStockFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PartStockInfo;
import com.fiap.techchallenge.inventory.api.representation.StockMovementInfo;
import com.fiap.techchallenge.inventory.entities.Part;
import com.fiap.techchallenge.inventory.entities.PartStock;
import com.fiap.techchallenge.inventory.exceptions.InvalidStockAdjustmentException;
import com.fiap.techchallenge.inventory.exceptions.PartNotFoundException;
import com.fiap.techchallenge.inventory.mappers.PartStockMapper;
import com.fiap.techchallenge.inventory.mappers.StockMovementMapper;
import com.fiap.techchallenge.inventory.repositories.PartRepository;
import com.fiap.techchallenge.inventory.repositories.PartStockRepository;
import com.fiap.techchallenge.inventory.repositories.StockMovementRepository;
import com.fiap.techchallenge.inventory.repositories.specifications.PartStockSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final PartRepository partRepository;
    private final StockMovementRepository movementRepository;
    private final PartStockRepository partStockRepository;

    private final StockLedger ledger;
    private final PartStockMapper partStockMapper;
    private final StockMovementMapper movementMapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public PartStockInfo adjust(UUID partId, AdjustStockCommand command) {
        if (command.quantity().signum() == 0) {
            throw new IllegalArgumentException("Adjustment quantity may not be zero");
        }

        // Locked so a concurrent reservation or another adjustment can't read a stale on-hand value.
        Part part = partRepository
                .findByIdForUpdate(partId)
                .orElseThrow(() -> new PartNotFoundException(partId));

        BigDecimal onHand = ledger.onHand(partId);
        BigDecimal reserved = ledger.reserved(partId);
        BigDecimal newOnHand = onHand.add(command.quantity());

        if (newOnHand.signum() < 0) {
            throw new InvalidStockAdjustmentException(
                    "Adjustment would drop on-hand quantity below zero for part " + partId);
        }

        if (newOnHand.compareTo(reserved) < 0) {
            throw new InvalidStockAdjustmentException(
                    "Adjustment would drop on-hand quantity below what is already reserved for part " + partId);
        }

        ledger.recordAdjustment(part, command.quantity(), command.reason());

        // An adjustment can move stock either way; letting the evaluator re-check unconditionally is
        // simpler than branching on the sign of a delta that rarely matters for cost.
        events.publishEvent(new PartPositionMayHaveDroppedEvent(partId));

        return getStock(partId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementInfo> getMovements(UUID partId, Pageable pageable) {
        return movementRepository
                .findByPartIdOrderByOccurredAtDesc(partId, pageable)
                .map(movementMapper::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public PartStockInfo getStock(UUID partId) {
        PartStock stock = partStockRepository
                .findById(partId)
                .orElseThrow(() -> new PartNotFoundException(partId));

        return partStockMapper.toInfo(stock);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartStockInfo> listStock(PartStockFilterQuery filter, Pageable pageable) {
        Specification<PartStock> spec = Specification
                .where(PartStockSpecifications.stockStatusEquals(filter.stockStatus()));

        return partStockRepository
                .findAll(spec, pageable)
                .map(partStockMapper::toInfo);
    }
}
