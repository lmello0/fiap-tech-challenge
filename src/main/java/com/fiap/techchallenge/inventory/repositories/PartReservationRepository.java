package com.fiap.techchallenge.inventory.repositories;

import com.fiap.techchallenge.inventory.entities.PartReservation;
import com.fiap.techchallenge.inventory.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PartReservationRepository extends JpaRepository<PartReservation, UUID> {

    List<PartReservation> findByWorkOrderIdAndStatus(UUID workOrderId, ReservationStatus status);

    List<PartReservation> findByStatusAndReservedAtBefore(ReservationStatus status, Instant cutoff);

    /** Oldest first, so a fresh receipt tops up the reservation that has been waiting the longest. */
    List<PartReservation> findByPart_IdAndStatusOrderByReservedAtAsc(UUID partId, ReservationStatus status);

    /** Most-recently-reserved first, so releasing a delta unwinds the most recent claim first. */
    List<PartReservation> findByWorkOrderIdAndPart_IdAndStatusOrderByReservedAtDesc(
            UUID workOrderId, UUID partId, ReservationStatus status);
}
