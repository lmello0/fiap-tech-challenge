package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.scheduling.api.events.AppointmentCheckedInEvent;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.CreateWorkOrderCommand;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import com.fiap.techchallenge.workorder.exceptions.IllegalWorkOrderTransitionException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Calls {@link AppointmentEventListener#handle} directly rather than going through Spring's
 * {@code ApplicationEvents} — {@code @ApplicationModuleListener}'s async dispatch is not reliably
 * observable that way (see {@code PickupInvitationRequestedListenerTest} for the same approach).
 */
class AppointmentEventListenerTest {

    private final WorkOrderService workOrderService = mock();
    private final AppointmentEventListener listener = new AppointmentEventListener(workOrderService);

    @Test
    void dropoffCheckInCreatesAWorkOrder() {
        UUID customerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        AppointmentCheckedInEvent event = event(AppointmentType.DROPOFF, customerId, vehicleId, null);

        listener.handle(event);

        verify(workOrderService).create(new CreateWorkOrderCommand(customerId, vehicleId, "Won't start"));
    }

    @Test
    void pickupCheckInDeliversTheWorkOrder() {
        UUID workOrderId = UUID.randomUUID();

        AppointmentCheckedInEvent event = event(AppointmentType.PICKUP, UUID.randomUUID(), UUID.randomUUID(), workOrderId);

        listener.handle(event);

        verify(workOrderService).deliver(workOrderId);
    }

    @Test
    void pickupCheckInSwallowsAnAlreadyDeliveredWorkOrder() {
        UUID workOrderId = UUID.randomUUID();
        when(workOrderService.deliver(workOrderId))
                .thenThrow(new IllegalWorkOrderTransitionException(WorkOrderStatus.DELIVERED, WorkOrderStatus.DELIVERED));

        AppointmentCheckedInEvent event = event(AppointmentType.PICKUP, UUID.randomUUID(), UUID.randomUUID(), workOrderId);

        listener.handle(event);

        verify(workOrderService, never()).create(any());
    }

    private static AppointmentCheckedInEvent event(AppointmentType type, UUID customerId, UUID vehicleId, UUID workOrderId) {
        return new AppointmentCheckedInEvent(
                UUID.randomUUID(),
                type,
                customerId,
                vehicleId,
                workOrderId,
                "Won't start",
                EventMetadata.system("AppointmentEventListenerTest", false),
                null
        );
    }
}
