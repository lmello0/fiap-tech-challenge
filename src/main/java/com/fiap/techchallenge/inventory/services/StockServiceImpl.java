package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.api.StockService;
import com.fiap.techchallenge.inventory.api.commands.AdjustStockCommand;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.api.representation.StockMovementInfo;
import com.fiap.techchallenge.inventory.entities.Part;
import com.fiap.techchallenge.inventory.entities.StockMovement;
import com.fiap.techchallenge.inventory.enums.StockMovementType;
import com.fiap.techchallenge.inventory.exceptions.PartNotFoundException;
import com.fiap.techchallenge.inventory.mappers.PartMapper;
import com.fiap.techchallenge.inventory.mappers.StockMovementMapper;
import com.fiap.techchallenge.inventory.repositories.PartRepository;
import com.fiap.techchallenge.inventory.repositories.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final PartRepository partRepository;
    private final StockMovementRepository movementRepository;

    private final PartMapper partMapper;
    private final StockMovementMapper movementMapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public PartInfo adjust(UUID partId, AdjustStockCommand command) {
        if (command.quantity().signum() == 0) {
            throw new IllegalArgumentException("Adjustment quantity may not be zero");
        }

        // Locked so a concurrent reservation or another adjustment can't read a stale on-hand value.
        Part part = partRepository
                .findByIdForUpdate(partId)
                .orElseThrow(() -> new PartNotFoundException(partId));

        part.applyAdjustment(command.quantity());

        StockMovement movement = new StockMovement();
        movement.setPart(part);
        movement.setType(StockMovementType.ADJUSTMENT);
        movement.setQuantity(command.quantity());
        movement.setReason(command.reason());

        movementRepository.save(movement);

        // An adjustment can move stock either way; letting the evaluator re-check unconditionally is
        // simpler than branching on the sign of a delta that rarely matters for cost.
        events.publishEvent(new PartPositionMayHaveDroppedEvent(partId));

        return partMapper.toInfo(part);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementInfo> getMovements(UUID partId, Pageable pageable) {
        return movementRepository
                .findByPartIdOrderByOccurredAtDesc(partId, pageable)
                .map(movementMapper::toInfo);
    }
}
