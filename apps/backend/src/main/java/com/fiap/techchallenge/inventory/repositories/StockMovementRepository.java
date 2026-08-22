package com.fiap.techchallenge.inventory.repositories;

import com.fiap.techchallenge.inventory.entities.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    Page<StockMovement> findByPartIdOrderByOccurredAtDesc(UUID partId, Pageable pageable);
}
