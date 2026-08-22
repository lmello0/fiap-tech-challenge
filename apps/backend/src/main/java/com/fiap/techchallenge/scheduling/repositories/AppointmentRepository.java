package com.fiap.techchallenge.scheduling.repositories;

import com.fiap.techchallenge.scheduling.entities.Appointment;
import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {

    long countByTypeAndSlotStartAndStatus(AppointmentType type, Instant slotStart, AppointmentStatus status);

    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.type = com.fiap.techchallenge.scheduling.enums.AppointmentType.DROPOFF
              AND a.status = com.fiap.techchallenge.scheduling.enums.AppointmentStatus.SCHEDULED
              AND a.customerId IS NULL
              AND (a.guestPhone = :phone OR a.guestEmail = :email)
            """)
    boolean existsActiveDropoffForGuestContact(@Param("phone") String phone, @Param("email") String email);

    List<Appointment> findByStatusAndSlotStartLessThan(AppointmentStatus status, Instant cutoff);

    List<Appointment> findByStatusAndSlotStartGreaterThanEqualAndSlotStartLessThan(
            AppointmentStatus status, Instant dayStart, Instant dayEnd);
}
