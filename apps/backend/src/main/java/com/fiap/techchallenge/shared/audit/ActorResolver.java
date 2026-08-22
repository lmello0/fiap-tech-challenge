package com.fiap.techchallenge.shared.audit;

import com.fiap.techchallenge.shared.logging.LogContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Builds the {@link EventMetadata} every published {@link DomainEvent} carries. Deliberately holds no
 * dependency on {@code auth} or {@code user} — the JWT subject claim is already the user's id
 * ({@code SecurityConfig#jwtAuthenticationConverter}), so no lookup is needed, and every module that
 * publishes a {@link DomainEvent} can call this without creating a cycle.
 */
@Component
public class ActorResolver {

    private static final String SYSTEM_STARTUP = "STARTUP";

    /** For a command handled inside an authenticated request. */
    public EventMetadata forCurrentUser(boolean customerVisible) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            return EventMetadata.system(SYSTEM_STARTUP, customerVisible);
        }

        UUID actorId;
        try {
            actorId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            return EventMetadata.system(authentication.getName(), customerVisible);
        }

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse(null);

        return new EventMetadata(
                UUID.randomUUID(), Instant.now(), ActorType.USER, actorId, role, customerVisible,
                LogContext.requestId());
    }

    /** For a scheduled job or startup task with no authenticated request behind it. */
    public EventMetadata forSystem(String jobName, boolean customerVisible) {
        return EventMetadata.system(jobName, customerVisible);
    }

    /**
     * For an action a person takes on their own account before a JWT exists to authenticate them —
     * registering, or confirming an email/password token link. The action is unmistakably theirs
     * (they hold the token or just supplied the registration data), just not one {@code
     * SecurityContextHolder} can see.
     */
    public EventMetadata forSelf(UUID actorId, boolean customerVisible) {
        return new EventMetadata(
                UUID.randomUUID(), Instant.now(), ActorType.USER, actorId, "SELF", customerVisible,
                LogContext.requestId());
    }
}
