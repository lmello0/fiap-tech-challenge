package com.fiap.techchallenge.scheduling.repositories;

import com.fiap.techchallenge.scheduling.entities.SchedulingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SchedulingSettingsRepository extends JpaRepository<SchedulingSettings, UUID> {

    /** Singleton row, seeded by the migration. */
    Optional<SchedulingSettings> findFirstByOrderByIdAsc();
}
