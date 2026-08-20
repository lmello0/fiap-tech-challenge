package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.scheduling.api.events.AppointmentCheckedInEvent;
import com.fiap.techchallenge.shared.logging.LogContext;
import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.CreateWorkOrderCommand;
import com.fiap.techchallenge.workorder.exceptions.IllegalWorkOrderTransitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * The {@code scheduling} &rarr; {@code workorder} half of ADR 0014's event-driven coordination: a
 * Drop-off Check-in creates the WorkOrder, a Pickup Check-in delivers it — never a synchronous call,
 * unlike {@code workorder}'s own dependency on {@code inventory} (ADR 0006). {@code workorder}
 * depends on {@code scheduling.api.events} here, the mirror image of {@code scheduling}'s dependency
 * on {@code workorder.api.events} for the WAITING_PICKUP invitation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class AppointmentEventListener {

    private final WorkOrderService workOrderService;

    @ApplicationModuleListener
    void on(AppointmentCheckedInEvent event) {
        try (LogContext.Scope ignored = LogContext.open(event.metadata().requestId())) {
            LogContext.put("appointmentId", event.aggregateId());
            LogContext.put("appointmentType", event.appointmentType());

            switch (event.appointmentType()) {
                case DROPOFF -> workOrderService.create(new CreateWorkOrderCommand(
                        event.customerId(), event.vehicleId(), event.complaint()));
                case PICKUP -> deliver(event);
            }

            log.info("appointment_checked_in");
        }
    }

    private void deliver(AppointmentCheckedInEvent event) {
        try {
            workOrderService.deliver(event.workOrderId());
        } catch (IllegalWorkOrderTransitionException e) {
            // Already delivered by a direct staff action before this Pickup check-in landed —
            // both paths lead to the same terminal state, so there's nothing left to do.
            LogContext.put("outcome", "already_delivered");
        }
    }
}
