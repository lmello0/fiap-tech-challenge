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
 * Sent when a Work Order reaches WAITING_PICKUP, before any Pickup Appointment exists — there is no
 * placeholder row to hang a token off (CONTEXT.md "Pickup Appointment"), so this token is keyed by
 * the Work Order instead. Single-use: consumed the moment a Pickup slot is actually booked.
 */
@Entity
@Table(name = "pickup_invitation_tokens", schema = "scheduling")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PickupInvitationToken {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID workOrderId;

    @Column(nullable = false, updatable = false)
    private UUID customerId;

    @Column(nullable = false, updatable = false)
    private UUID vehicleId;

    @Column(nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    private Instant usedAt;

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }
}
