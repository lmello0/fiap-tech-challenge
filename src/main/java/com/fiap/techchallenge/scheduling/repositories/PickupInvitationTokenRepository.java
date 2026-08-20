package com.fiap.techchallenge.scheduling.repositories;

import com.fiap.techchallenge.scheduling.entities.PickupInvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PickupInvitationTokenRepository extends JpaRepository<PickupInvitationToken, UUID> {

    Optional<PickupInvitationToken> findByTokenHash(String tokenHash);
}
