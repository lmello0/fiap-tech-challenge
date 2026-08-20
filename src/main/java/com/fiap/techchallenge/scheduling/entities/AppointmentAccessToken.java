package com.fiap.techchallenge.scheduling.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * The Booking-Management Token (CONTEXT.md): lets a Guest view, cancel, or reschedule their
 * Appointment without logging in. Not single-use — unlike the Complete-Registration Token, this one
 * may be needed more than once before the Appointment resolves.
 */
@Entity
@Table(name = "appointment_access_tokens", schema = "scheduling")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AppointmentAccessToken {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID appointmentId;

    @Column(nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }
}
