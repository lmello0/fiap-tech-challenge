package com.fiap.techchallenge.email.api;

import com.fiap.techchallenge.shared.logging.LogContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailDeliveredEventTest {

    @Test
    void aNullRequestIdInheritsWhicheverUnitOfWorkIsCurrentlyOpen() {
        try (LogContext.Scope ignored = LogContext.open("the-open-request-id")) {
            EmailDeliveredEvent event = new EmailDeliveredEvent(UUID.randomUUID(), null);

            assertThat(event.requestId()).isEqualTo("the-open-request-id");
        }
    }

    @Test
    void aNullRequestIdStaysNullOutsideAnyOpenUnitOfWork() {
        EmailDeliveredEvent event = new EmailDeliveredEvent(UUID.randomUUID(), null);

        assertThat(event.requestId()).isNull();
    }

    @Test
    void anExplicitRequestIdIsKept() {
        EmailDeliveredEvent event = new EmailDeliveredEvent(UUID.randomUUID(), "explicit-request-id");

        assertThat(event.requestId()).isEqualTo("explicit-request-id");
    }

    @Test
    void theTwoArgConstructorDefaultsRequestIdTheSameWay() {
        UUID correlationId = UUID.randomUUID();

        try (LogContext.Scope ignored = LogContext.open("the-open-request-id")) {
            EmailDeliveredEvent event = new EmailDeliveredEvent(correlationId);

            assertThat(event.correlationId()).isEqualTo(correlationId);
            assertThat(event.requestId()).isEqualTo("the-open-request-id");
        }
    }
}
