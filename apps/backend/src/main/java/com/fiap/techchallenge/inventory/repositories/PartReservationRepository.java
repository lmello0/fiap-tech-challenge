package com.fiap.techchallenge.inventory.repositories;

import com.fiap.techchallenge.inventory.entities.PartReservation;
import com.fiap.techchallenge.inventory.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    /** What a part currently has claimed against it — the "reserved" half of {@code available}. */
    @Query("""
            select coalesce(sum(r.quantityReserved), 0)
            from PartReservation r
            where r.part.id = :partId and r.status = com.fiap.techchallenge.inventory.enums.ReservationStatus.HELD
            """)
    BigDecimal sumReservedForPart(@Param("partId") UUID partId);

    /** A work order's reservations that still can't be fully satisfied — what blocks it from starting service. */
    @Query("""
            select r from PartReservation r
            where r.workOrderId = :workOrderId
              and r.status = com.fiap.techchallenge.inventory.enums.ReservationStatus.HELD
              and r.quantityReserved < r.quantityRequested
            """)
    List<PartReservation> findBlockingShortfalls(@Param("workOrderId") UUID workOrderId);

    /** How many of a work order's reservations are still short, used to report progress as shortfalls heal. */
    @Query("""
            select count(r) from PartReservation r
            where r.workOrderId = :workOrderId
              and r.status = com.fiap.techchallenge.inventory.enums.ReservationStatus.HELD
              and r.quantityReserved < r.quantityRequested
            """)
    long countBlockingShortfalls(@Param("workOrderId") UUID workOrderId);
}
