package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.PartReservationService;
import com.fiap.techchallenge.inventory.api.RepairServiceCatalogService;
import com.fiap.techchallenge.inventory.api.commands.ReservePartCommand;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
import com.fiap.techchallenge.shared.audit.ActorResolver;
import com.fiap.techchallenge.vehicle.api.VehicleService;
import com.fiap.techchallenge.vehicle.api.representation.VehicleInfo;
import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.*;
import com.fiap.techchallenge.workorder.api.events.*;
import com.fiap.techchallenge.workorder.api.queries.WorkOrderFilterQuery;
import com.fiap.techchallenge.workorder.api.representation.CustomerWorkOrderView;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderSnapshot;
import com.fiap.techchallenge.workorder.entities.Budget;
import com.fiap.techchallenge.workorder.entities.BudgetLine;
import com.fiap.techchallenge.workorder.entities.WorkOrder;
import com.fiap.techchallenge.workorder.enums.BudgetStatus;
import com.fiap.techchallenge.workorder.enums.RowType;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import com.fiap.techchallenge.workorder.exceptions.*;
import com.fiap.techchallenge.workorder.mappers.BudgetMapper;
import com.fiap.techchallenge.workorder.mappers.WorkOrderMapper;
import com.fiap.techchallenge.workorder.repositories.BudgetRepository;
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

import java.time.Duration;
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
    private final BudgetRepository budgetRepository;
    private final WorkOrderStateMachine stateMachine;
    private final ApplicationEventPublisher events;
    private final ActorResolver actorResolver;

    private final WorkOrderMapper woMapper;
    private final BudgetMapper budgetMapper;

    private final PartCatalogService partCatalogService;
    private final RepairServiceCatalogService repairServiceCatalogService;
    private final PartReservationService partReservationService;
    private final VehicleService vehicleService;

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

    @Transactional(readOnly = true)
    public CustomerWorkOrderView getForCustomer(UUID workOrderId, UUID customerId) {
        WorkOrder wo = load(workOrderId);

        if (!wo.getCustomerId().equals(customerId)) {
            throw new WorkOrderNotFoundException(workOrderId);
        }

        Budget budget = wo.getCurrentBudget();

        return new CustomerWorkOrderView(
                wo.getId(),
                wo.getOrderCode(),
                wo.getStatus(),
                budget == null ? null : budgetMapper.toInfo(budget)
        );
    }

    @Transactional
    public WorkOrderInfo create(CreateWorkOrderCommand command) {
        VehicleInfo vehicle = vehicleService.getById(command.vehicleId());

        if (!vehicle.customerId().equals(command.customerId())) {
            throw new VehicleOwnershipException(command.vehicleId(), command.customerId());
        }

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
                Instant.now(),
                actorResolver.forCurrentUser(true),
                snapshot(wo)
        ));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo requestDiagnostics(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.WAITING_DIAGNOSTICS));
        wo.setDiagnosticRequestedAt(Instant.now());

        events.publishEvent(new WorkOrderDiagnosticsRequestedEvent(
                wo.getId(), wo.getVehicleId(), actorResolver.forCurrentUser(true), snapshot(wo)));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo startDiagnostics(UUID workOrderId, StartDiagnosticsCommand command) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.IN_DIAGNOSTICS));
        wo.setAssignedMechanicId(command.mechanicId());
        wo.setDiagnosticStartedAt(Instant.now());

        events.publishEvent(new WorkOrderDiagnosticsStartedEvent(
                wo.getId(), wo.getVehicleId(), command.mechanicId(), actorResolver.forCurrentUser(false), snapshot(wo)));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo finishDiagnostics(UUID workOrderId, FinishDiagnosticsCommand command) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.BUDGET_IN_DRAFT));
        wo.setDiagnosis(command.diagnosis());
        wo.setDiagnosticFinishedAt(Instant.now());

        Budget budget = new Budget();
        budget.setStatus(BudgetStatus.DRAFT);
        wo.addBudget(budget);

        command.lines().forEach(lineCommand -> budget.addLine(buildLine(lineCommand)));

        // Flushed, not just saved: each BudgetLine's id is only assigned once its INSERT actually
        // runs, and the snapshot captured below for BudgetDraftedEvent needs those ids populated --
        // otherwise History's per-line entries get written with a null entity_id and fail their
        // not-null constraint (see BudgetServiceImpl#addLine for the same fix).
        budgetRepository.saveAndFlush(budget);

        // Best-effort: claims what stock exists now, records the rest as a shortfall, and still lets
        // the quote go out. The physical constraint is enforced later, strictly, at startService.
        partReservationService.reserveForWorkOrder(wo.getId(), partReservationRequests(budget));

        events.publishEvent(new BudgetDraftedEvent(
                wo.getId(), budget.getId(), actorResolver.forCurrentUser(false), snapshot(wo)));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo startService(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.IN_PROGRESS));

        // Strict: throws if any reservation is still short, which rolls back this whole transaction —
        // service never starts on parts that aren't actually on the shelf.
        partReservationService.consumeForWorkOrder(wo.getId());

        wo.setServiceStartedAt(Instant.now());

        events.publishEvent(new WorkOrderInProgressEvent(wo.getId(), actorResolver.forCurrentUser(true), snapshot(wo)));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo startLine(UUID workOrderId, UUID lineId) {
        WorkOrder wo = load(workOrderId);
        requireInProgress(wo);

        BudgetLine line = findServiceLine(wo, lineId);

        if (line.getStartedAt() != null) {
            throw new IllegalArgumentException("Line " + lineId + " has already been started");
        }

        line.setStartedAt(Instant.now());

        events.publishEvent(new BudgetLineStartedEvent(
                wo.getId(), line.getId(), actorResolver.forCurrentUser(false), snapshot(wo)));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo finishLine(UUID workOrderId, UUID lineId) {
        WorkOrder wo = load(workOrderId);
        requireInProgress(wo);

        BudgetLine line = findServiceLine(wo, lineId);

        if (line.getStartedAt() == null) {
            throw new IllegalArgumentException("Line " + lineId + " has not been started");
        }
        if (line.getFinishedAt() != null) {
            throw new IllegalArgumentException("Line " + lineId + " has already been finished");
        }

        Instant finishedAt = Instant.now();
        line.setFinishedAt(finishedAt);

        // Clamped to at least 1s: the DB requires a positive duration, and an operation finished in
        // the same instant it started (a trivially fast service, or a test) shouldn't be rejected for it.
        long elapsed = Duration.between(line.getStartedAt(), finishedAt).getSeconds();
        int durationSeconds = (int) Math.max(1, elapsed);

        repairServiceCatalogService.recordExecution(line.getServiceId(), wo.getId(), line.getId(), durationSeconds);

        events.publishEvent(new BudgetLineFinishedEvent(
                wo.getId(), line.getId(), actorResolver.forCurrentUser(false), snapshot(wo)));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo finish(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.FINISHED));
        wo.setFinishedAt(Instant.now());

        events.publishEvent(new WorkOrderFinishedEvent(
                wo.getId(), wo.getCustomerId(), actorResolver.forCurrentUser(true), snapshot(wo)));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo waitingPickup(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.WAITING_PICKUP));
        wo.setPickupReadyAt(Instant.now());

        events.publishEvent(new WorkOrderWaitingPickupEvent(wo.getId(), actorResolver.forCurrentUser(true), snapshot(wo)));

        return woMapper.toInfo(wo);
    }

    @Transactional
    public WorkOrderInfo deliver(UUID workOrderId) {
        WorkOrder wo = load(workOrderId);

        wo.setStatus(stateMachine.transition(wo.getStatus(), WorkOrderStatus.DELIVERED));
        wo.setDeliveredAt(Instant.now());

        events.publishEvent(new WorkOrderDeliveredEvent(
                wo.getId(), wo.getCustomerId(), actorResolver.forCurrentUser(true), snapshot(wo)));

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

    private void requireInProgress(WorkOrder wo) {
        if (wo.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new WorkOrderNotInProgressException(wo.getId(), wo.getStatus());
        }
    }

    private BudgetLine findServiceLine(WorkOrder wo, UUID lineId) {
        Budget budget = wo.getCurrentBudget();

        if (budget == null) {
            throw new BudgetLineNotFoundException(lineId);
        }

        BudgetLine line = budget.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new BudgetLineNotFoundException(lineId));

        if (line.getType() != RowType.SERVICE) {
            throw new IllegalArgumentException("Line " + lineId + " is not a SERVICE line");
        }

        return line;
    }

    /**
     * Description and unit price are never taken from the caller — they are snapshotted from the
     * inventory catalog here, so a later price change never rewrites what a customer already agreed
     * to.
     */
    private BudgetLine buildLine(AddBudgetLineCommand command) {
        BudgetLine line = new BudgetLine();
        line.setType(command.type());
        line.setQuantity(command.quantity());
        line.setPartId(command.partId());
        line.setServiceId(command.serviceId());

        if (command.type() == RowType.PART) {
            if (command.partId() == null || command.serviceId() != null) {
                throw new IllegalArgumentException("A PART line must supply partId and no serviceId");
            }

            PartInfo part = partCatalogService.getById(command.partId());
            line.setDescription(part.name());
            line.setUnitPrice(part.salePrice());
        } else {
            if (command.serviceId() == null || command.partId() != null) {
                throw new IllegalArgumentException("A SERVICE line must supply serviceId and no partId");
            }

            RepairServiceInfo service = repairServiceCatalogService.getById(command.serviceId());
            line.setDescription(service.name());
            line.setUnitPrice(service.price());
        }

        return line;
    }

    private List<ReservePartCommand> partReservationRequests(Budget budget) {
        return budget.getLines().stream()
                .filter(l -> l.getType() == RowType.PART)
                .map(l -> new ReservePartCommand(l.getPartId(), l.getQuantity()))
                .toList();
    }

    private WorkOrderSnapshot snapshot(WorkOrder wo) {
        Budget current = wo.getCurrentBudget();
        return new WorkOrderSnapshot(woMapper.toInfo(wo), current != null ? budgetMapper.toInfo(current) : null);
    }
}
