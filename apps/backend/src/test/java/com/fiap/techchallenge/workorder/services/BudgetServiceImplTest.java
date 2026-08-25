package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.email.api.EmailDeliveredEvent;
import com.fiap.techchallenge.email.api.EmailDeliveryFailedEvent;
import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.PartReservationService;
import com.fiap.techchallenge.inventory.api.RepairServiceCatalogService;
import com.fiap.techchallenge.shared.audit.ActorResolver;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.workorder.entities.Budget;
import com.fiap.techchallenge.workorder.entities.WorkOrder;
import com.fiap.techchallenge.workorder.enums.BudgetStatus;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import com.fiap.techchallenge.workorder.mappers.BudgetMapper;
import com.fiap.techchallenge.workorder.mappers.WorkOrderMapper;
import com.fiap.techchallenge.workorder.notifications.WorkOrderEmails;
import com.fiap.techchallenge.workorder.repositories.BudgetRepository;
import com.fiap.techchallenge.workorder.repositories.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Targets the two {@code @ApplicationModuleListener} methods directly -- no live Mailpit or Spring
 * context involved, same reasoning as {@code AppointmentEventListenerTest}: calling a plain,
 * package-visible method bypasses any async-dispatch/testability concerns entirely.
 */
class BudgetServiceImplTest {

    private final BudgetRepository budgetRepository = mock();
    private final WorkOrderRepository workOrderRepository = mock();
    private final BudgetStateMachine budgetStateMachine = new BudgetStateMachine();
    private final WorkOrderStateMachine workOrderStateMachine = new WorkOrderStateMachine();
    private final ApplicationEventPublisher events = mock();
    private final ActorResolver actorResolver = mock();
    private final BudgetMapper budgetMapper = mock();
    private final WorkOrderMapper workOrderMapper = mock();
    private final PartCatalogService partCatalogService = mock();
    private final RepairServiceCatalogService repairServiceCatalogService = mock();
    private final PartReservationService partReservationService = mock();
    private final UserService userService = mock();
    private final BudgetDecisionTokenService tokenService = mock();
    private final WorkOrderEmails workOrderEmails = mock();

    private final BudgetServiceImpl service = new BudgetServiceImpl(
            budgetRepository, workOrderRepository, budgetStateMachine, workOrderStateMachine,
            events, actorResolver, budgetMapper, workOrderMapper,
            partCatalogService, repairServiceCatalogService, partReservationService, userService,
            tokenService, workOrderEmails);

    @Test
    void emailDeliveredMovesAWaitingSendBudgetToSent() {
        UUID workOrderId = UUID.randomUUID();
        Budget budget = budget(BudgetStatus.WAITING_SEND, workOrderId);
        WorkOrder wo = workOrder(workOrderId, WorkOrderStatus.BUDGET_IN_DRAFT);

        when(budgetRepository.findById(budget.getId())).thenReturn(Optional.of(budget));
        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo));

        service.on(new EmailDeliveredEvent(budget.getId()));

        org.assertj.core.api.Assertions.assertThat(budget.getStatus()).isEqualTo(BudgetStatus.SENT);
        org.assertj.core.api.Assertions.assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.WAITING_APPROVAL);
        verify(events).publishEvent(org.mockito.ArgumentMatchers.any(
                com.fiap.techchallenge.workorder.api.events.BudgetSentEvent.class));
    }

    @Test
    void emailDeliveredForABudgetNoLongerWaitingSendIsIgnored() {
        UUID workOrderId = UUID.randomUUID();
        Budget budget = budget(BudgetStatus.SENT, workOrderId);

        when(budgetRepository.findById(budget.getId())).thenReturn(Optional.of(budget));

        service.on(new EmailDeliveredEvent(budget.getId()));

        org.assertj.core.api.Assertions.assertThat(budget.getStatus()).isEqualTo(BudgetStatus.SENT);
        verify(workOrderRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        verify(events, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void emailDeliveredForAnUnknownBudgetIsANoOp() {
        UUID budgetId = UUID.randomUUID();
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());

        service.on(new EmailDeliveredEvent(budgetId));

        verify(workOrderRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void emailDeliveryFailedIsJustLogged() {
        Budget budget = budget(BudgetStatus.WAITING_SEND, UUID.randomUUID());
        when(budgetRepository.findById(budget.getId())).thenReturn(Optional.of(budget));

        service.on(new EmailDeliveryFailedEvent(budget.getId(), "SMTP timeout"));

        verify(events, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    private static Budget budget(BudgetStatus status, UUID workOrderId) {
        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setStatus(status);

        WorkOrder wo = new WorkOrder();
        wo.setId(workOrderId);
        budget.setWorkOrder(wo);

        return budget;
    }

    private static WorkOrder workOrder(UUID id, WorkOrderStatus status) {
        WorkOrder wo = new WorkOrder();
        wo.setId(id);
        wo.setStatus(status);
        wo.setCustomerId(UUID.randomUUID());

        return wo;
    }
}
