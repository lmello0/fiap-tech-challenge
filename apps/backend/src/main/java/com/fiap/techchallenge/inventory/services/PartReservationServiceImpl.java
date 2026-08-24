package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.api.PartReservationService;
import com.fiap.techchallenge.inventory.api.commands.ReservePartCommand;
import com.fiap.techchallenge.inventory.api.events.PartPositionMayHaveDroppedEvent;
import com.fiap.techchallenge.inventory.api.events.PartReservationExpiredEvent;
import com.fiap.techchallenge.inventory.api.representation.BlockingShortfallInfo;
import com.fiap.techchallenge.inventory.api.representation.PartReservationSnapshot;
import com.fiap.techchallenge.inventory.entities.Part;
import com.fiap.techchallenge.inventory.entities.PartReservation;
import com.fiap.techchallenge.inventory.enums.ReservationStatus;
import com.fiap.techchallenge.inventory.exceptions.InsufficientStockException;
import com.fiap.techchallenge.inventory.exceptions.PartNotFoundException;
import com.fiap.techchallenge.inventory.repositories.PartRepository;
import com.fiap.techchallenge.inventory.repositories.PartReservationRepository;
import com.fiap.techchallenge.shared.audit.ActorResolver;
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
    private final StockLedger ledger;
    private final ApplicationEventPublisher events;
    private final ReservationStateMachine stateMachine;
    private final ActorResolver actorResolver;

    @Override
    @Transactional
    public void reserveForWorkOrder(UUID workOrderId, List<ReservePartCommand> requests) {
        for (ReservePartCommand request : requests) {
            Part part = loadForUpdate(request.partId());

            BigDecimal reserved = request.quantity().min(ledger.available(part.getId()));
            reserved = reserved.max(BigDecimal.ZERO);

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

            // Claiming stock lowers available (and therefore position); re-check the stock policy.
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
                // No lock/movement needed: releasing only unwinds this reservation row's own claim,
                // and available is derived from it directly — nothing physical moved.
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

            ledger.recordConsumption(part, reservation.getQuantityReserved(), workOrderId);

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
                events.publishEvent(new PartReservationExpiredEvent(
                        workOrderId, partId, released,
                        actorResolver.forSystem("ExpireStaleReservations", false),
                        new PartReservationSnapshot(workOrderId, partId, released)));
            }
        }

        return stale.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockingShortfallInfo> getBlockingShortfalls(UUID workOrderId) {
        return reservationRepository.findBlockingShortfalls(workOrderId).stream()
                .map(r -> new BlockingShortfallInfo(
                        r.getPart().getId(), r.getPart().getSku(), r.getPart().getName(), r.getShortfall()))
                .toList();
    }

    private List<PartReservation> held(UUID workOrderId) {
        return reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.HELD);
    }

    private void resolve(PartReservation reservation, ReservationStatus resolution) {
        // Releasing/expiring only unwinds the reservation row itself; nothing physical moved, so no
        // lock or movement is needed — available rises the moment quantityReserved drops.
        reservation.setStatus(stateMachine.transition(reservation.getStatus(), resolution));
        reservation.setResolvedAt(Instant.now());
    }

    private Part loadForUpdate(UUID partId) {
        return partRepository
                .findByIdForUpdate(partId)
                .orElseThrow(() -> new PartNotFoundException(partId));
    }
}
