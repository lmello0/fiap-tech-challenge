package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.scheduling.api.events.PickupInvitationRequestedEvent;
import com.fiap.techchallenge.shared.audit.ActorResolver;
import com.fiap.techchallenge.workorder.api.events.WorkOrderWaitingPickupEvent;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Republishes {@code workorder}'s own WAITING_PICKUP event as a {@code scheduling}-owned one, so
 * {@code scheduling} never has to depend on a {@code workorder} type (ADR 0014) — the dependency this
 * creates points from {@code workorder} to {@code scheduling.api.events}, the same direction as the
 * Check-in coordination.
 */
@Component
@RequiredArgsConstructor
class PickupInvitationTrigger {

    private final ApplicationEventPublisher events;
    private final ActorResolver actorResolver;

    @ApplicationModuleListener
    void on(WorkOrderWaitingPickupEvent event) {
        WorkOrderInfo workOrder = event.snapshot().workOrder();

        events.publishEvent(new PickupInvitationRequestedEvent(
                workOrder.id(),
                workOrder.customerId(),
                workOrder.vehicleId(),
                actorResolver.forSystem("WorkOrderWaitingPickup", true)
        ));
    }
}
