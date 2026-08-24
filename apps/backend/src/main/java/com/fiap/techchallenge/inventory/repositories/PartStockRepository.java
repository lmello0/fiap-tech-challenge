package com.fiap.techchallenge.inventory.repositories;

import com.fiap.techchallenge.inventory.entities.PartStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/** Read-only access to the {@code inventory.part_stock} view. Never save/delete — see {@link PartStock}. */
public interface PartStockRepository extends JpaRepository<PartStock, UUID>, JpaSpecificationExecutor<PartStock> {
}
