package com.fiap.techchallenge.inventory.repositories;

import com.fiap.techchallenge.inventory.entities.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    Page<StockMovement> findByPartIdOrderByOccurredAtDesc(UUID partId, Pageable pageable);

    /** On-hand for a part: the ledger's only source of truth for it. */
    @Query("select coalesce(sum(m.quantity), 0) from StockMovement m where m.part.id = :partId")
    BigDecimal sumQuantityForPart(@Param("partId") UUID partId);
}
