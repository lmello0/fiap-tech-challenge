package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.api.RepairServiceCatalogService;
import com.fiap.techchallenge.inventory.api.commands.CreateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.queries.RepairServiceFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
import com.fiap.techchallenge.inventory.entities.RepairService;
import com.fiap.techchallenge.inventory.entities.ServiceExecution;
import com.fiap.techchallenge.inventory.exceptions.RepairServiceNotFoundException;
import com.fiap.techchallenge.inventory.mappers.RepairServiceMapper;
import com.fiap.techchallenge.inventory.properties.InventoryProperties;
import com.fiap.techchallenge.inventory.repositories.RepairServiceRepository;
import com.fiap.techchallenge.inventory.repositories.ServiceExecutionRepository;
import com.fiap.techchallenge.inventory.repositories.specifications.RepairServiceSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairServiceCatalogServiceImpl implements RepairServiceCatalogService {

    private final RepairServiceRepository repairServiceRepository;
    private final ServiceExecutionRepository executionRepository;
    private final RepairServiceMapper repairServiceMapper;
    private final InventoryProperties properties;

    @Override
    @Transactional(readOnly = true)
    public Page<RepairServiceInfo> listServices(RepairServiceFilterQuery filter, Pageable pageable) {
        Specification<RepairService> spec = Specification
                .where(RepairServiceSpecifications.codeEquals(filter.code()))
                .and(RepairServiceSpecifications.nameContains(filter.name()))
                .and(RepairServiceSpecifications.activeEquals(filter.active()));

        return repairServiceRepository
                .findAll(spec, pageable)
                .map(repairServiceMapper::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public RepairServiceInfo getById(UUID id) {
        return repairServiceRepository
                .findById(id)
                .map(repairServiceMapper::toInfo)
                .orElseThrow(() -> new RepairServiceNotFoundException(id));
    }

    @Override
    @Transactional
    public RepairServiceInfo create(CreateRepairServiceCommand command) {
        RepairService repairService = new RepairService();

        repairService.setCode(command.code());
        repairService.setName(command.name());
        repairService.setDescription(command.description());
        repairService.setPrice(command.price());
        repairService.setEstimatedSeconds(command.estimatedSeconds());

        repairServiceRepository.save(repairService);

        return repairServiceMapper.toInfo(repairService);
    }

    @Override
    @Transactional
    public RepairServiceInfo update(UUID id, UpdateRepairServiceCommand command) {
        RepairService repairService = load(id);

        repairService.setName(command.name());
        repairService.setDescription(command.description());
        repairService.setPrice(command.price());
        repairService.setEstimatedSeconds(command.estimatedSeconds());

        return repairServiceMapper.toInfo(repairService);
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        RepairService repairService = load(id);

        repairService.deactivate();
    }

    @Override
    @Transactional
    public void recordExecution(UUID repairServiceId, UUID workOrderId, UUID workOrderRowId, int durationSeconds) {
        RepairService repairService = load(repairServiceId);

        ServiceExecution execution = new ServiceExecution();
        execution.setRepairService(repairService);
        execution.setWorkOrderId(workOrderId);
        execution.setWorkOrderRowId(workOrderRowId);
        execution.setDurationSeconds(durationSeconds);

        executionRepository.save(execution);

        List<ServiceExecution> recent = executionRepository.findByRepairService_IdOrderByRecordedAtDesc(
                repairServiceId, PageRequest.of(0, properties.averageWindow()));

        int average = (int) Math.round(
                recent.stream().mapToInt(ServiceExecution::getDurationSeconds).average().orElseThrow());

        repairService.setAverageSeconds(average);
        repairService.setExecutionCount(repairService.getExecutionCount() + 1);
    }

    private RepairService load(UUID id) {
        return repairServiceRepository
                .findById(id)
                .orElseThrow(() -> new RepairServiceNotFoundException(id));
    }
}
