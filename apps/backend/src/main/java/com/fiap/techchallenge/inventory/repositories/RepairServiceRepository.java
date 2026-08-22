package com.fiap.techchallenge.inventory.repositories;

import com.fiap.techchallenge.inventory.entities.RepairService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface RepairServiceRepository extends JpaRepository<RepairService, UUID>, JpaSpecificationExecutor<RepairService> {
}
