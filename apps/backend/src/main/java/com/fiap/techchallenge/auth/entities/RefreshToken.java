package com.fiap.techchallenge.auth.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", schema = "auth")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    private UUID replacedBy;

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean wasRotated() {
        return replacedBy != null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public void revoke(Instant when, UUID replacedBy) {
        this.revokedAt = when;
        this.replacedBy = replacedBy;
    }

    public void revoke(Instant when) {
        this.revokedAt = when;
    }
}
