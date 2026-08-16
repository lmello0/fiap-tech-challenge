package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.*;
import com.fiap.techchallenge.workorder.api.events.*;
import com.fiap.techchallenge.workorder.exceptions.WorkOrderNotFoundException;
import com.fiap.techchallenge.workorder.api.queries.WorkOrderFilterQuery;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import com.fiap.techchallenge.workorder.entities.WorkOrder;
import com.fiap.techchallenge.workorder.entities.WorkOrderRow;
import com.fiap.techchallenge.workorder.enums.RowType;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import com.fiap.techchallenge.workorder.mappers.WorkOrderMapper;
import com.fiap.techchallenge.workorder.mappers.WorkOrderRowMapper;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderRepository repository;
    private final WorkOrderStateMachine stateMachine;
    private final ApplicationEventPublisher events;

    private final WorkOrderMapper woMapper;
    private final WorkOrderRowMapper worMapper;

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
                .map(woMapper::toInfo);
    }

    @Transactional(readOnly = true)
    public WorkOrderInfo getWorkOrderById(UUID id) {
        return repository
                .findById(id)
                .map(woMapper::toInfo)
                .orElseThrow(() -> new WorkOrderNotFoundException(id));
    }

    @Transactional
    public WorkOrderInfo create(CreateWorkOrderCommand command) {
        WorkOrder wo = new WorkOrder();
        wo.setOrderCode(nextOrderNumber());
        wo.setCustomerId(command.customerId());
        wo.setVehicleId(command.vehicleId());
        wo.setCustomerComplaint(command.complaint());
        wo.setStatus(WorkOrderStatus.RECEIVED);

        repository.save(wo);

        events.publishEvent(new WorkOrderCreatedEvent(
                wo.getId(),
                wo.getOrderCode(),
                command.customerId(),
                command.vehicleId(),
                Instant.now()
        ));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo requestDiagnostics(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.WAITING_DIAGNOSTICS));
        wo.setDiagnosticRequestedAt(Instant.now());

        events.publishEvent(new WorkOrderDiagnosticsRequestedEvent(wo.getId(), wo.getVehicleId()));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo startDiagnostics(UUID workOrderId, StartDiagnosticsCommand command) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.IN_DIAGNOSTICS));
        wo.setAssignedMechanicId(command.mechanicId());
        wo.setDiagnosticStartedAt(Instant.now());

        events.publishEvent(new WorkOrderDiagnosticsStartedEvent(wo.getId(), wo.getVehicleId(), command.mechanicId()));

        return woMapper.toInfo(wo);
    }

    @Transactional
    WorkOrderInfo finishDiagnostics(UUID workOrderId, FinishDiagnosticsCommand command) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.WAITING_APPROVAL));
        wo.setDiagnosis(command.diagnosis());
        wo.setDiagnosticFinishedAt(Instant.now());

        replaceRows(wo, command.rows());
        recalculateTotals(wo);

        events.publishEvent(new WorkOrderWaitingApprovalEvent(wo.getId(), wo.getCustomerId(), wo.getGrandTotal()));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo approve(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.APPROVED));
        wo.setApprovedAt(Instant.now());

        events.publishEvent(new WorkOrderApprovedEvent(wo.getId(), wo.getCustomerId(), wo.getGrandTotal()));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo refuse(UUID workOrderId, RefuseWorkOrderCommand command) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.REFUSED));
        wo.setRefusalReason(command.reason());
        wo.setRefusedAt(Instant.now());

        events.publishEvent(new WorkOrderRefusedEvent(wo.getId(), wo.getCustomerId(), command.reason()));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo startService(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.IN_PROGRESS));
        wo.setServiceStartedAt(Instant.now());

        events.publishEvent(new WorkOrderInProgressEvent(wo.getId()));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo finish(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.FINISHED));
        wo.setFinishedAt(Instant.now());

        events.publishEvent(new WorkOrderFinishedEvent(wo.getId(), wo.getCustomerId()));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo waitingPickup(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.WAITING_PICKUP));
        wo.setPickupReadyAt(Instant.now());

        events.publishEvent(new WorkOrderWaitingPickupEvent(wo.getId()));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo deliver(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.DELIVERED));
        wo.setDeliveredAt(Instant.now());

        events.publishEvent(new WorkOrderDeliveredEvent(wo.getId(), wo.getCustomerId()));

        return woMapper.toInfo(wo);
    }

    private String nextOrderNumber() {
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

    private void replaceRows(WorkOrder wo, List<CreateWorkOrderRowCommand> rowCommands) {
        wo.clearRows();

        wo.setRows(
                rowCommands.stream()
                        .map(worMapper::fromCreateCommand)
                        .toList()
        );
    }

    private void recalculateTotals(WorkOrder wo) {
        BigDecimal labor = sumRows(wo, RowType.LABOR);
        BigDecimal parts = sumRows(wo, RowType.PART);
        BigDecimal total = labor.add(parts);

        wo.setLaborTotal(labor);
        wo.setPartsTotal(parts);
        wo.setGrandTotal(total);
    }

    private BigDecimal sumRows(WorkOrder wo, RowType type) {
        return wo.getRows().stream()
                .filter(r -> r.getType() == type)
                .map(WorkOrderRow::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
