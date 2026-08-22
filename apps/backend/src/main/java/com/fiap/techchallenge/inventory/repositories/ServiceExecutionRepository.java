package com.fiap.techchallenge.inventory.repositories;

import com.fiap.techchallenge.inventory.entities.ServiceExecution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceExecutionRepository extends JpaRepository<ServiceExecution, UUID> {

    List<ServiceExecution> findByRepairService_IdOrderByRecordedAtDesc(UUID repairServiceId, Pageable pageable);
}
