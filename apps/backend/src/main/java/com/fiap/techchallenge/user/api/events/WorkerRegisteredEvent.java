package com.fiap.techchallenge.user.api.events;

import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.user.enums.WorkerRole;

import java.util.UUID;

public record WorkerRegisteredEvent(
        UUID userId,
        WorkerRole role,
        EventMetadata metadata,
        UserInfo snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "WORKER_REGISTERED";
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
