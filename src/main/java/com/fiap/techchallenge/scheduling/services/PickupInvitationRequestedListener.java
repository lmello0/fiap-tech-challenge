package com.fiap.techchallenge.scheduling.services;

import com.fiap.techchallenge.scheduling.api.events.PickupInvitationRequestedEvent;
import com.fiap.techchallenge.scheduling.notifications.SchedulingEmails;
import com.fiap.techchallenge.shared.logging.LogContext;
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
        handle(event);
    }

    /**
     * Split from {@link #on} so it can be driven without Modulith's async dispatch (see
     * {@code email.schedules.RetryFailedEmails} for the original precedent, applied here to
     * {@code @ApplicationModuleListener} instead of {@code @Scheduled}): calling {@code on} directly
     * still goes through the proxy that makes it asynchronous, since the interception is keyed to the
     * method itself, not how it's invoked.
     */
    void handle(PickupInvitationRequestedEvent event) {
        try (LogContext.Scope ignored = LogContext.open(event.metadata().requestId())) {
            LogContext.put("workOrderId", event.workOrderId());
            LogContext.put("customerId", event.customerId());

            String rawToken = tokenService.issuePickupInvitationToken(
                    event.workOrderId(), event.customerId(), event.vehicleId());

            String email = userService.findById(event.customerId())
                    .map(UserInfo::email)
                    .orElse(null);

            if (email == null) {
                LogContext.put("outcome", "customer_vanished");
                log.warn("pickup_invitation_requested");
                return;
            }

            emails.pickupInvitation(email, rawToken);

            LogContext.put("outcome", "sent");
            log.info("pickup_invitation_requested");
        }
    }
}
