package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.api.PartReservationService;
import com.fiap.techchallenge.inventory.api.commands.ReservePartCommand;
import com.fiap.techchallenge.inventory.api.events.PartReservationExpiredEvent;
import com.fiap.techchallenge.inventory.entities.Part;
import com.fiap.techchallenge.inventory.entities.PartReservation;
import com.fiap.techchallenge.inventory.entities.StockMovement;
import com.fiap.techchallenge.inventory.enums.ReservationStatus;
import com.fiap.techchallenge.inventory.enums.StockMovementType;
import com.fiap.techchallenge.inventory.exceptions.InsufficientStockException;
import com.fiap.techchallenge.inventory.exceptions.PartNotFoundException;
import com.fiap.techchallenge.inventory.repositories.PartRepository;
import com.fiap.techchallenge.inventory.repositories.PartReservationRepository;
import com.fiap.techchallenge.inventory.repositories.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartReservationServiceImpl implements PartReservationService {

    private final PartRepository partRepository;
    private final PartReservationRepository reservationRepository;
    private final StockMovementRepository movementRepository;
    private final ApplicationEventPublisher events;
    private final ReservationStateMachine stateMachine;

    @Override
    @Transactional
    public void reserveForWorkOrder(UUID workOrderId, List<ReservePartCommand> requests) {
        for (ReservePartCommand request : requests) {
            Part part = loadForUpdate(request.partId());

            BigDecimal reserved = part.reserve(request.quantity());

            PartReservation reservation = new PartReservation();
            reservation.setPart(part);
            reservation.setWorkOrderId(workOrderId);
            reservation.setQuantityRequested(request.quantity());
            reservation.setQuantityReserved(reserved);
            reservation.setStatus(ReservationStatus.HELD);

            reservationRepository.save(reservation);

            if (reserved.compareTo(request.quantity()) < 0) {
                log.warn("Reservation shortfall: work order {} part {} requested {} got {}",
                        workOrderId, request.partId(), request.quantity(), reserved);
            }

            // Claiming stock lowers available (and therefore position); re-check the reorder rule.
            events.publishEvent(new PartPositionMayHaveDroppedEvent(request.partId()));
        }
    }

    @Override
    @Transactional
    public void releasePartial(UUID workOrderId, UUID partId, BigDecimal quantity) {
        BigDecimal remaining = quantity;

        List<PartReservation> reservations = reservationRepository
                .findByWorkOrderIdAndPart_IdAndStatusOrderByReservedAtDesc(workOrderId, partId, ReservationStatus.HELD);

        for (PartReservation reservation : reservations) {
            if (remaining.signum() <= 0) {
                break;
            }

            BigDecimal amount = reservation.getQuantityReserved().min(remaining);

            if (amount.signum() > 0) {
                Part part = loadForUpdate(partId);
                part.releaseReservation(amount);

                reservation.setQuantityReserved(reservation.getQuantityReserved().subtract(amount));
                remaining = remaining.subtract(amount);
            }

            reservation.setQuantityRequested(
                    reservation.getQuantityRequested().subtract(amount.min(reservation.getQuantityRequested())));

            if (reservation.getQuantityReserved().signum() == 0 && reservation.getQuantityRequested().signum() == 0) {
                reservation.setStatus(stateMachine.transition(reservation.getStatus(), ReservationStatus.RELEASED));
                reservation.setResolvedAt(Instant.now());
            }
        }
    }

    @Override
    @Transactional
    public void releaseForWorkOrder(UUID workOrderId) {
        for (PartReservation reservation : held(workOrderId)) {
            resolve(reservation, ReservationStatus.RELEASED);
        }
    }

    @Override
    @Transactional
    public void consumeForWorkOrder(UUID workOrderId) {
        List<PartReservation> reservations = held(workOrderId);

        Map<UUID, BigDecimal> shortfalls = reservations.stream()
                .filter(PartReservation::hasShortfall)
                .collect(Collectors.toMap(r -> r.getPart().getId(), PartReservation::getShortfall));

        if (!shortfalls.isEmpty()) {
            throw new InsufficientStockException(workOrderId, shortfalls);
        }

        for (PartReservation reservation : reservations) {
            Part part = loadForUpdate(reservation.getPart().getId());

            part.consumeReservation(reservation.getQuantityReserved());

            StockMovement movement = new StockMovement();
            movement.setPart(part);
            movement.setType(StockMovementType.CONSUMPTION);
            movement.setQuantity(reservation.getQuantityReserved().negate());
            movement.setReferenceId(workOrderId);

            movementRepository.save(movement);

            reservation.setStatus(stateMachine.transition(reservation.getStatus(), ReservationStatus.CONSUMED));
            reservation.setResolvedAt(Instant.now());
        }
    }

    @Override
    @Transactional
    public int expireStaleReservations(Instant cutoff) {
        List<PartReservation> stale =
                reservationRepository.findByStatusAndReservedAtBefore(ReservationStatus.HELD, cutoff);

        for (PartReservation reservation : stale) {
            BigDecimal released = reservation.getQuantityReserved();
            UUID partId = reservation.getPart().getId();
            UUID workOrderId = reservation.getWorkOrderId();

            resolve(reservation, ReservationStatus.EXPIRED);

            if (released.signum() > 0) {
                events.publishEvent(new PartReservationExpiredEvent(workOrderId, partId, released));
            }
        }

        return stale.size();
    }

    private List<PartReservation> held(UUID workOrderId) {
        return reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.HELD);
    }

    private void resolve(PartReservation reservation, ReservationStatus resolution) {
        if (reservation.getQuantityReserved().signum() > 0) {
            Part part = loadForUpdate(reservation.getPart().getId());

            part.releaseReservation(reservation.getQuantityReserved());
        }

        reservation.setStatus(stateMachine.transition(reservation.getStatus(), resolution));
        reservation.setResolvedAt(Instant.now());
    }

    private Part loadForUpdate(UUID partId) {
        return partRepository
                .findByIdForUpdate(partId)
                .orElseThrow(() -> new PartNotFoundException(partId));
    }
}
