package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.events.*;
import com.fiap.techchallenge.workorder.api.exceptions.WorkOrderNotFoundException;
import com.fiap.techchallenge.workorder.api.queries.WorkOrderFilterQuery;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import com.fiap.techchallenge.workorder.entities.WorkOrder;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import com.fiap.techchallenge.workorder.mappers.WorkOrderMapper;
import com.fiap.techchallenge.workorder.repositories.WorkOrderRepository;
import com.fiap.techchallenge.workorder.repositories.specifications.WorkOrderSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderRepository repository;
    private final WorkOrderStateMachine stateMachine;
    private final ApplicationEventPublisher events;
    private final WorkOrderMapper mapper;

    @Transactional(readOnly = true)
    public Page<WorkOrderInfo> getAllWorkOrders(WorkOrderFilterQuery filter, Pageable pageable) {
        Specification<WorkOrder> spec = Specification
                .where(WorkOrderSpecifications.belongsToCustomer(filter.customerId()))
                .and(WorkOrderSpecifications.ofVehicle(filter.vehicleId()))
                .and(WorkOrderSpecifications.withMechanic(filter.mechanicId()))
                .and(WorkOrderSpecifications.withStatus(filter.status()))
                .and(WorkOrderSpecifications.withCode(filter.code()))
                .and(WorkOrderSpecifications.createdBetween(filter.createdAt(), filter.finishedAt()));

        return repository
                .findAll(spec, pageable)
                .map(mapper::toInfo);
    }

    @Transactional(readOnly = true)
    public WorkOrderInfo getWorkOrderById(UUID id) {
        return repository
                .findById(id)
                .map(mapper::toInfo)
                .orElseThrow(() -> new WorkOrderNotFoundException(id));
    }

    @Transactional
    public WorkOrderInfo create(
            UUID customerId,
            UUID vehicleId,
            String complaint
    ) {
        WorkOrder wo = new WorkOrder();
        wo.setOrderCode(nextOrderNumber());
        wo.setCustomerId(customerId);
        wo.setVehicleId(vehicleId);
        wo.setCustomerComplaint(complaint);
        wo.setStatus(WorkOrderStatus.RECEIVED);
        wo.setCreatedAt(Instant.now());

        repository.save(wo);

        events.publishEvent(new WorkOrderCreatedEvent(
                wo.getId(),
                wo.getOrderCode(),
                customerId,
                vehicleId,
                Instant.now()
        ));

        return mapper.toInfo(wo);
    }

    @Transactional
    public void requestDiagnostics(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.WAITING_DIAGNOSTICS));

        events.publishEvent(new DiagnosticsRequestedEvent(wo.getId(), wo.getVehicleId()));
    }

    @Transactional
    public void approve(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.APPROVED));
        wo.setApprovedAt(Instant.now());

        events.publishEvent(new WorkOrderApprovedEvent(wo.getId(), wo.getCustomerId(), wo.getGrandTotal()));
    }

    @Transactional
    public void refuse(UUID workOrderId, String reason) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.REFUSED));
        wo.setRefusalReason(reason);
        wo.setRefusedAt(Instant.now());

        events.publishEvent(new WorkOrderRefusedEvent(wo.getId(), wo.getCustomerId(), reason));
    }

    @Transactional
    public void finish(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.FINISHED));
        wo.setFinishedAt(Instant.now());

        events.publishEvent(new WorkOrderFinishedEvent(wo.getId(), wo.getCustomerId()));
    }

    @Transactional
    public void deliver(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.DELIVERED));
        wo.setDeliveredAt(Instant.now());

        events.publishEvent(new WorkOrderDeliveredEvent(wo.getId(), wo.getCustomerId()));
    }

    @Transactional(readOnly = true)
    protected String nextOrderNumber() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDateTime now = LocalDateTime.now();
        String formattedDate = dtf.format(now);

        Long nextSeq = repository.getNextSequence();

        return "WO-" + formattedDate + "-%06d".formatted(nextSeq);
    }

    private WorkOrder load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new WorkOrderNotFoundException(id));
    }
}
