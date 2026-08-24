package com.fiap.techchallenge.inventory.repositories;

import com.fiap.techchallenge.inventory.entities.StockPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface StockPolicyRepository extends JpaRepository<StockPolicy, UUID>, JpaSpecificationExecutor<StockPolicy> {

    Optional<StockPolicy> findByPart_Id(UUID partId);
}
