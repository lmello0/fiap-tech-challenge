package com.fiap.techchallenge.scheduling.repositories;

import com.fiap.techchallenge.scheduling.entities.AppointmentRegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppointmentRegistrationTokenRepository extends JpaRepository<AppointmentRegistrationToken, UUID> {

    Optional<AppointmentRegistrationToken> findByTokenHash(String tokenHash);
}
