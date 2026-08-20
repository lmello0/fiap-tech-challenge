package com.fiap.techchallenge.user.api.events;

import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.user.api.representation.UserInfo;

import java.time.LocalDate;
import java.util.UUID;

public record WorkerTerminatedEvent(
        UUID userId,
        LocalDate terminationDate,
        EventMetadata metadata,
        UserInfo snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "WORKER_TERMINATED";
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
