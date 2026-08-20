package com.fiap.techchallenge.scheduling.services;

import com.fiap.techchallenge.scheduling.api.events.PickupInvitationRequestedEvent;
import com.fiap.techchallenge.scheduling.notifications.SchedulingEmails;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to {@code workorder}'s WAITING_PICKUP transition (relayed as {@link
 * PickupInvitationRequestedEvent}, which {@code scheduling} itself owns and publishes-to — see that
 * event's Javadoc and ADR 0014): issues a Pickup Invitation Token and emails the booking link. Purely
 * internal to {@code scheduling} — this listener imports nothing from {@code workorder}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class PickupInvitationRequestedListener {

    private final AppointmentTokenService tokenService;
    private final SchedulingEmails emails;
    private final UserService userService;

    @ApplicationModuleListener
    void on(PickupInvitationRequestedEvent event) {
        String rawToken = tokenService.issuePickupInvitationToken(
                event.workOrderId(), event.customerId(), event.vehicleId());

        String email = userService.findById(event.customerId())
                .map(UserInfo::email)
                .orElse(null);

        if (email == null) {
            log.warn("Cannot send pickup invitation, customer vanished workOrderId={} customerId={}",
                    event.workOrderId(), event.customerId());
            return;
        }

        emails.pickupInvitation(email, rawToken);
    }
}
