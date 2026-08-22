package com.fiap.techchallenge.auth.repositories;

import com.fiap.techchallenge.auth.entities.EmailChangeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, UUID> {

    Optional<EmailChangeToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE EmailChangeToken t SET t.usedAt = :now WHERE t.userId = :userId AND t.usedAt IS NULL")
    void invalidatePendingForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
