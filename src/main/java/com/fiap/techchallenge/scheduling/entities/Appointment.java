package com.fiap.techchallenge.scheduling.entities;

import com.fiap.techchallenge.scheduling.enums.AppointmentCancelReason;
import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A booked visit for a specific slot — either a Drop-off or a Pickup (CONTEXT.md "Appointment"). A
 * single table for both kinds: a Drop-off carries either a Guest snapshot or a real customer/vehicle
 * id, a Pickup always carries a real customer/vehicle id plus the {@code workOrderId} it belongs to.
 */
@Entity
@Table(name = "appointments", schema = "scheduling")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Appointment {

    public static final Duration SLOT_DURATION = Duration.ofMinutes(30);

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private AppointmentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AppointmentStatus status;

    @Column(nullable = false, updatable = false)
    private Instant slotStart;

    private UUID customerId;

    private UUID vehicleId;

    @Column(length = 100)
    private String guestName;

    @Column(length = 30)
    private String guestPhone;

    @Column(length = 255)
    private String guestEmail;

    @Column(length = 50)
    private String guestVehicleMake;

    @Column(length = 100)
    private String guestVehicleModel;

    private Integer guestVehicleYear;

    @Column(length = 2000)
    private String complaint;

    private UUID workOrderId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AppointmentCancelReason cancelReason;

    @Column(length = 500)
    private String cancelMessage;

    private UUID rescheduledToId;

    private Instant checkedInAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public Instant slotEnd() {
        return slotStart.plus(SLOT_DURATION);
    }

    public boolean isGuest() {
        return customerId == null;
    }

    public void checkIn() {
        this.status = AppointmentStatus.COMPLETED;
        this.checkedInAt = Instant.now();
    }

    public void cancel(AppointmentCancelReason reason, String message) {
        this.status = AppointmentStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelMessage = message;
    }

    public void markNoShow() {
        this.status = AppointmentStatus.NO_SHOW;
    }

    public void linkReschedule(UUID newAppointmentId) {
        this.rescheduledToId = newAppointmentId;
    }

    public void completeGuestConversion(UUID customerId, UUID vehicleId) {
        this.customerId = customerId;
        this.vehicleId = vehicleId;
    }
}
