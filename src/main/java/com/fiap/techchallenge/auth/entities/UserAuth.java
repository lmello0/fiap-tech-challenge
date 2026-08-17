package com.fiap.techchallenge.auth.entities;

import com.fiap.techchallenge.auth.enums.AuthProvider;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_auths", schema = "auth")
@IdClass(UserAuthId.class)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class UserAuth {

    @Id
    @Column(nullable = false)
    private UUID userId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AuthProvider provider;

    private @Nullable String providerId;

    private @Nullable String passwordHash;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private @Nullable Instant modifiedAt;

    @Column(nullable = false)
    private int failedAttempts;

    private @Nullable Instant lockedUntil;

    @Column(nullable = false)
    private boolean needPasswordChange;

    private UserAuth(UUID userId, AuthProvider provider, @Nullable String passwordHash, boolean needPasswordChange) {
        this.userId = userId;
        this.provider = provider;
        this.passwordHash = passwordHash;
        this.needPasswordChange = needPasswordChange;
    }

    public static UserAuth local(UUID userId, String passwordHash) {
        return new UserAuth(userId, AuthProvider.LOCAL, passwordHash, false);
    }

    /**
     * For accounts whose first password was chosen by someone other than their holder — a manager
     * onboarding a worker, or the bootstrap runner reading one out of the environment. The holder
     * must rotate it before the account can be used for anything else.
     */
    public static UserAuth localWithTemporaryPassword(UUID userId, String passwordHash) {
        return new UserAuth(userId, AuthProvider.LOCAL, passwordHash, true);
    }

    /**
     * The single choke point that clears {@code needPasswordChange}: both the authenticated change
     * and the emailed reset funnel through here, so neither can leave the flag standing.
     */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.modifiedAt = Instant.now();
        this.needPasswordChange = false;
        resetFailedAttempts();
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void registerFailedAttempt(Instant now, int maxAttempts, Duration lockDuration) {
        this.failedAttempts++;
        if (this.failedAttempts >= maxAttempts) {
            this.lockedUntil = now.plus(lockDuration);
        }
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }
}
