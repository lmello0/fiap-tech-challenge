package com.fiap.techchallenge.user.api.events;

import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.user.api.representation.UserInfo;

import java.util.UUID;

public record UserProfileUpdatedEvent(
        UUID userId,
        EventMetadata metadata,
        UserInfo snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "USER_PROFILE_UPDATED";
    }

    @Override
    public String aggregateType() {
        return UserAggregate.TYPE;
    }

    @Override
    public UUID aggregateId() {
        return userId;
    }
}
