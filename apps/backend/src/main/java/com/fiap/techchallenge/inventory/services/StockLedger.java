package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.api.events.WorkOrderPartsReplenishedEvent;
import com.fiap.techchallenge.inventory.entities.Part;
import com.fiap.techchallenge.inventory.entities.PartReservation;
import com.fiap.techchallenge.inventory.entities.PurchaseOrderLine;
import com.fiap.techchallenge.inventory.entities.StockMovement;
import com.fiap.techchallenge.inventory.enums.ReservationStatus;
import com.fiap.techchallenge.inventory.enums.StockMovementType;
import com.fiap.techchallenge.inventory.repositories.PartReservationRepository;
import com.fiap.techchallenge.inventory.repositories.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The only place that writes a {@link StockMovement} or reads a part's derived on-hand/available.
 * Every caller that reserves, consumes, receives, or adjusts stock goes through here rather than
 * touching {@link Part} or the repositories directly — on-hand is {@code SUM(movements.quantity)},
 * never a column, so there is nowhere else this arithmetic should live.
 *
 * <p>Callers are responsible for taking {@code PartRepository.findByIdForUpdate}'s pessimistic lock
 * on the part row <em>before</em> calling any write method here — the part row carries no quantity of
 * its own, but locking it is still what makes two concurrent claims on the last unit of a part
 * serialize instead of racing.
 */
@Component
@RequiredArgsConstructor
class StockLedger {

    private final StockMovementRepository movementRepository;
    private final PartReservationRepository reservationRepository;
    private final ApplicationEventPublisher events;

    BigDecimal onHand(UUID partId) {
        return movementRepository.sumQuantityForPart(partId);
    }

    BigDecimal reserved(UUID partId) {
        return reservationRepository.sumReservedForPart(partId);
    }

    BigDecimal available(UUID partId) {
        return onHand(partId).subtract(reserved(partId));
    }

    void recordPurchase(Part part, BigDecimal quantity, BigDecimal unitCost, PurchaseOrderLine line) {
        StockMovement movement = new StockMovement();
        movement.setPart(part);
        movement.setType(StockMovementType.PURCHASE);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        movement.setPurchaseOrderLine(line);
        movementRepository.saveAndFlush(movement);

        healShortfalls(part);
    }

    void recordConsumption(Part part, BigDecimal quantity, UUID workOrderId) {
        StockMovement movement = new StockMovement();
        movement.setPart(part);
        movement.setType(StockMovementType.CONSUMPTION);
        movement.setQuantity(quantity.negate());
        movement.setWorkOrderId(workOrderId);
        movementRepository.saveAndFlush(movement);
    }

    void recordAdjustment(Part part, BigDecimal delta, String reason) {
        StockMovement movement = new StockMovement();
        movement.setPart(part);
        movement.setType(StockMovementType.ADJUSTMENT);
        movement.setQuantity(delta);
        movement.setReason(reason);
        // Flushed immediately: a caller reading the part_stock view right after this call (e.g. to
        // return the new standing from an adjustment endpoint) must see this row, and the view has
        // no way to read uncommitted-but-unflushed state from the persistence context.
        movementRepository.saveAndFlush(movement);

        if (delta.signum() > 0) {
            healShortfalls(part);
        }
    }

    /**
     * Tops up open shortfalls for a part FIFO by {@code reservedAt} — the reservation that has been
     * waiting longest is satisfied first, regardless of whether its work order is a draft or already
     * approved (see ADR 0019: inventory can't see budget state, so it can't prioritize by it). Publishes
     * one {@link WorkOrderPartsReplenishedEvent} per work order touched, on every partial heal.
     */
    void healShortfalls(Part part) {
        List<PartReservation> waiting = reservationRepository
                .findByPart_IdAndStatusOrderByReservedAtAsc(part.getId(), ReservationStatus.HELD)
                .stream()
                .filter(PartReservation::hasShortfall)
                .toList();

        Map<UUID, List<UUID>> healedPartsByWorkOrder = new LinkedHashMap<>();

        for (PartReservation reservation : waiting) {
            BigDecimal avail = available(part.getId());

            if (avail.signum() <= 0) {
                break;
            }

            BigDecimal topUp = reservation.getShortfall().min(avail);

            if (topUp.signum() > 0) {
                reservation.setQuantityReserved(reservation.getQuantityReserved().add(topUp));
                healedPartsByWorkOrder
                        .computeIfAbsent(reservation.getWorkOrderId(), id -> new java.util.ArrayList<>())
                        .add(part.getId());
            }
        }

        healedPartsByWorkOrder.forEach((workOrderId, healedPartIds) -> {
            long remaining = reservationRepository.countBlockingShortfalls(workOrderId);

            events.publishEvent(new WorkOrderPartsReplenishedEvent(workOrderId, healedPartIds, (int) remaining));
        });
    }
}
