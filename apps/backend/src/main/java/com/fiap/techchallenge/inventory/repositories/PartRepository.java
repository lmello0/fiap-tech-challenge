package com.fiap.techchallenge.inventory.repositories;

import com.fiap.techchallenge.inventory.entities.Part;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PartRepository extends JpaRepository<Part, UUID>, JpaSpecificationExecutor<Part> {

    /**
     * Locked read for reserve/consume/adjust/receive: two concurrent claims on the last unit of a
     * part must never both succeed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Part p where p.id = :id")
    Optional<Part> findByIdForUpdate(UUID id);
}
