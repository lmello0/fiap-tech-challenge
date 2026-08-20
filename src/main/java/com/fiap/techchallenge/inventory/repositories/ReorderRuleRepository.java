package com.fiap.techchallenge.inventory.repositories;

import com.fiap.techchallenge.inventory.entities.ReorderRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ReorderRuleRepository extends JpaRepository<ReorderRule, UUID>, JpaSpecificationExecutor<ReorderRule> {

    Optional<ReorderRule> findByPart_Id(UUID partId);
}
