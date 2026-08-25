package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.email.api.EmailDeliveredEvent;
import com.fiap.techchallenge.email.api.EmailDeliveryFailedEvent;
import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.PartReservationService;
import com.fiap.techchallenge.inventory.api.RepairServiceCatalogService;
import com.fiap.techchallenge.inventory.api.commands.ReservePartCommand;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
import com.fiap.techchallenge.shared.audit.ActorResolver;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.shared.logging.LogContext;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.workorder.api.BudgetService;
import com.fiap.techchallenge.workorder.api.commands.AddBudgetLineCommand;
import com.fiap.techchallenge.workorder.api.commands.ChangeBudgetLineQuantityCommand;
import com.fiap.techchallenge.workorder.api.commands.RefuseWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.events.BudgetLineAddedEvent;
import com.fiap.techchallenge.workorder.api.events.BudgetLineQuantityChangedEvent;
import com.fiap.techchallenge.workorder.api.events.BudgetLineRemovedEvent;
import com.fiap.techchallenge.workorder.api.events.BudgetSentEvent;
import com.fiap.techchallenge.workorder.api.events.WorkOrderApprovedEvent;
import com.fiap.techchallenge.workorder.api.events.WorkOrderRefusedEvent;
import com.fiap.techchallenge.workorder.api.representation.BudgetInfo;
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
import com.fiap.techchallenge.workorder.notifications.WorkOrderEmails;
import com.fiap.techchallenge.workorder.repositories.BudgetRepository;
import com.fiap.techchallenge.workorder.repositories.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final WorkOrderRepository workOrderRepository;
    private final BudgetStateMachine budgetStateMachine;
    private final WorkOrderStateMachine workOrderStateMachine;
    private final ApplicationEventPublisher events;
    private final ActorResolver actorResolver;
    private final BudgetMapper budgetMapper;
    private final WorkOrderMapper workOrderMapper;

    private final PartCatalogService partCatalogService;
    private final RepairServiceCatalogService repairServiceCatalogService;
    private final PartReservationService partReservationService;
    private final UserService userService;
    private final BudgetDecisionTokenService tokenService;
    private final WorkOrderEmails workOrderEmails;

    @Transactional(readOnly = true)
    public BudgetInfo getById(UUID budgetId) {
        return budgetMapper.toInfo(load(budgetId));
    }

    @Transactional
    public BudgetInfo addLine(UUID budgetId, AddBudgetLineCommand command) {
        Budget budget = load(budgetId);
        requireDraft(budget);

        BudgetLine transientLine = buildLine(command);
        budget.addLine(transientLine);

        // budget is already managed (loaded above), so save() merges rather than persists it -- and
        // merge() cascades by replacing each new, still-transient child with a newly managed copy
        // rather than attaching transientLine itself. Its id is never assigned; the flushed,
        // now-generated id has to be read back from budget's own collection instead.
        budgetRepository.saveAndFlush(budget);
        BudgetLine line = budget.getLines().get(budget.getLines().size() - 1);

        if (line.getType() == RowType.PART) {
            partReservationService.reserveForWorkOrder(
                    budget.getWorkOrderId(), List.of(new ReservePartCommand(line.getPartId(), line.getQuantity())));
        }

        events.publishEvent(new BudgetLineAddedEvent(
                budget.getWorkOrderId(), budget.getId(), line.getId(),
                actorResolver.forCurrentUser(false), snapshot(budget)));

        return budgetMapper.toInfo(budget);
    }

    @Transactional
    public BudgetInfo removeLine(UUID budgetId, UUID lineId) {
        Budget budget = load(budgetId);
        requireDraft(budget);

        BudgetLine line = findLine(budget, lineId);
        budget.removeLine(line);

        if (line.getType() == RowType.PART) {
            partReservationService.releasePartial(budget.getWorkOrderId(), line.getPartId(), line.getQuantity());
        }

        events.publishEvent(new BudgetLineRemovedEvent(
                budget.getWorkOrderId(), budget.getId(), lineId,
                actorResolver.forCurrentUser(false), snapshot(budget)));

        return budgetMapper.toInfo(budget);
    }

    @Transactional
    public BudgetInfo changeLineQuantity(UUID budgetId, UUID lineId, ChangeBudgetLineQuantityCommand command) {
        Budget budget = load(budgetId);
        requireDraft(budget);

        BudgetLine line = findLine(budget, lineId);
        BigDecimal oldQuantity = line.getQuantity();
        BigDecimal newQuantity = command.quantity();
        BigDecimal delta = newQuantity.subtract(oldQuantity);

        line.setQuantity(newQuantity);

        if (line.getType() == RowType.PART && delta.signum() != 0) {
            if (delta.signum() > 0) {
                partReservationService.reserveForWorkOrder(
                        budget.getWorkOrderId(), List.of(new ReservePartCommand(line.getPartId(), delta)));
            } else {
                partReservationService.releasePartial(budget.getWorkOrderId(), line.getPartId(), delta.negate());
            }
        }

        events.publishEvent(new BudgetLineQuantityChangedEvent(
                budget.getWorkOrderId(), budget.getId(), lineId, oldQuantity, newQuantity,
                actorResolver.forCurrentUser(false), snapshot(budget)));

        return budgetMapper.toInfo(budget);
    }

    @Transactional
    public BudgetInfo send(UUID budgetId) {
        Budget budget = load(budgetId);

        budget.setStatus(budgetStateMachine.transition(budget.getStatus(), BudgetStatus.WAITING_SEND));

        dispatchBudgetEmail(budget);

        return budgetMapper.toInfo(budget);
    }

    @Transactional
    public BudgetInfo resend(UUID budgetId) {
        Budget budget = load(budgetId);

        if (budget.getStatus() != BudgetStatus.WAITING_SEND) {
            throw new IllegalBudgetTransitionException(budget.getStatus(), BudgetStatus.WAITING_SEND);
        }

        dispatchBudgetEmail(budget);

        return budgetMapper.toInfo(budget);
    }

    @Transactional
    public BudgetInfo approve(UUID budgetId, UUID customerId) {
        Budget budget = load(budgetId);
        WorkOrder wo = loadWorkOrder(budget.getWorkOrderId());
        requireOwner(wo, customerId);

        return applyApprove(budget, wo, actorResolver.forCurrentUser(true));
    }

    @Transactional
    public BudgetInfo refuse(UUID budgetId, UUID customerId, RefuseWorkOrderCommand command) {
        Budget budget = load(budgetId);
        WorkOrder wo = loadWorkOrder(budget.getWorkOrderId());
        requireOwner(wo, customerId);

        return applyRefuse(budget, wo, command, actorResolver.forCurrentUser(true));
    }

    @Transactional(readOnly = true)
    public BudgetInfo viewByToken(String rawToken) {
        return budgetMapper.toInfo(load(tokenService.resolve(rawToken)));
    }

    @Transactional
    public BudgetInfo approveByToken(String rawToken) {
        Budget budget = load(tokenService.resolve(rawToken));
        WorkOrder wo = loadWorkOrder(budget.getWorkOrderId());

        return applyApprove(budget, wo, actorResolver.forSelf(wo.getCustomerId(), true));
    }

    @Transactional
    public BudgetInfo refuseByToken(String rawToken, RefuseWorkOrderCommand command) {
        Budget budget = load(tokenService.resolve(rawToken));
        WorkOrder wo = loadWorkOrder(budget.getWorkOrderId());

        return applyRefuse(budget, wo, command, actorResolver.forSelf(wo.getCustomerId(), true));
    }

    private BudgetInfo applyApprove(Budget budget, WorkOrder wo, EventMetadata actor) {
        budget.setStatus(budgetStateMachine.transition(budget.getStatus(), BudgetStatus.APPROVED));
        budget.setResolvedAt(Instant.now());

        wo.setStatus(workOrderStateMachine.transition(wo.getStatus(), WorkOrderStatus.APPROVED));
        wo.setApprovedAt(Instant.now());

        events.publishEvent(new WorkOrderApprovedEvent(
                wo.getId(), wo.getCustomerId(), budget.getGrandTotal(), actor, snapshot(wo, budget)));

        return budgetMapper.toInfo(budget);
    }

    private BudgetInfo applyRefuse(Budget budget, WorkOrder wo, RefuseWorkOrderCommand command, EventMetadata actor) {
        budget.setStatus(budgetStateMachine.transition(budget.getStatus(), BudgetStatus.REFUSED));
        budget.setResolvedAt(Instant.now());

        wo.setStatus(workOrderStateMachine.transition(wo.getStatus(), WorkOrderStatus.REFUSED));
        wo.setRefusalReason(command.reason());
        wo.setRefusedAt(Instant.now());

        // A refusal moves no metal: every reservation the quote held is returned to availability, and
        // no stock movement is written since nothing physical happened (ADR 0007).
        partReservationService.releaseForWorkOrder(wo.getId());

        events.publishEvent(new WorkOrderRefusedEvent(
                wo.getId(), wo.getCustomerId(), command.reason(), actor, snapshot(wo, budget)));

        return budgetMapper.toInfo(budget);
    }

    /** Delivery-confirmation listener: only Budget-sent emails carry a correlation id we recognize. */
    @ApplicationModuleListener
    void on(EmailDeliveredEvent event) {
        try (LogContext.Scope ignored = LogContext.open(event.requestId())) {
            budgetRepository.findById(event.correlationId()).ifPresent(budget -> {
                LogContext.put("budgetId", budget.getId());

                if (budget.getStatus() != BudgetStatus.WAITING_SEND) {
                    LogContext.put("outcome", "ignored");
                    log.info("budget_email_delivered");
                    return;
                }

                budget.setStatus(budgetStateMachine.transition(budget.getStatus(), BudgetStatus.SENT));
                budget.setSentAt(Instant.now());

                WorkOrder wo = loadWorkOrder(budget.getWorkOrderId());
                wo.setStatus(workOrderStateMachine.transition(wo.getStatus(), WorkOrderStatus.WAITING_APPROVAL));
                workOrderRepository.save(wo);

                events.publishEvent(new BudgetSentEvent(
                        wo.getId(), budget.getId(), wo.getCustomerId(), budget.getGrandTotal(),
                        actorResolver.forSystem("EmailDispatcher", true), snapshot(wo, budget)));

                LogContext.put("outcome", "sent");
                log.info("budget_email_delivered");
            });
        }
    }

    @ApplicationModuleListener
    void on(EmailDeliveryFailedEvent event) {
        try (LogContext.Scope ignored = LogContext.open(event.requestId())) {
            budgetRepository.findById(event.correlationId()).ifPresent(budget -> {
                LogContext.put("budgetId", budget.getId());
                LogContext.put("reason", event.reason());
                log.warn("budget_email_delivery_failed");
            });
        }
    }

    private void dispatchBudgetEmail(Budget budget) {
        WorkOrder wo = loadWorkOrder(budget.getWorkOrderId());
        UserInfo customer = userService.getById(wo.getCustomerId());
        String vehicleDescription = "%s — %s %s".formatted(wo.getVehiclePlate(), wo.getVehicleMake(), wo.getVehicleModel());
        String rawToken = tokenService.issue(budget.getId());

        workOrderEmails.budgetReadyForApproval(
                customer.email(), budget.getId(), wo.getOrderCode(), vehicleDescription, budget.getGrandTotal(), rawToken);
    }

    private Budget load(UUID id) {
        return budgetRepository.findById(id).orElseThrow(() -> new BudgetNotFoundException(id));
    }

    private WorkOrder loadWorkOrder(UUID id) {
        return workOrderRepository.findById(id).orElseThrow(() -> new WorkOrderNotFoundException(id));
    }

    private void requireDraft(Budget budget) {
        if (budget.isLocked()) {
            throw new BudgetLockedException(budget.getId());
        }
    }

    private void requireOwner(WorkOrder wo, UUID customerId) {
        if (!wo.getCustomerId().equals(customerId)) {
            throw new WorkOrderNotFoundException(wo.getId());
        }
    }

    private BudgetLine findLine(Budget budget, UUID lineId) {
        return budget.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new BudgetLineNotFoundException(lineId));
    }

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

    private WorkOrderSnapshot snapshot(Budget budget) {
        return snapshot(loadWorkOrder(budget.getWorkOrderId()), budget);
    }

    private WorkOrderSnapshot snapshot(WorkOrder wo, Budget budget) {
        return new WorkOrderSnapshot(workOrderMapper.toInfo(wo), budgetMapper.toInfo(budget));
    }
}
