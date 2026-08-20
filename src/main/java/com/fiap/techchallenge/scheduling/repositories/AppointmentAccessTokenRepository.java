package com.fiap.techchallenge.scheduling.repositories;

import com.fiap.techchallenge.scheduling.entities.AppointmentAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppointmentAccessTokenRepository extends JpaRepository<AppointmentAccessToken, UUID> {

    Optional<AppointmentAccessToken> findByTokenHash(String tokenHash);
}
