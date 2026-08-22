package com.fiap.techchallenge.email.api;

import com.fiap.techchallenge.shared.logging.LogContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailDeliveryFailedEventTest {

    @Test
    void aNullRequestIdInheritsWhicheverUnitOfWorkIsCurrentlyOpen() {
        try (LogContext.Scope ignored = LogContext.open("the-open-request-id")) {
            EmailDeliveryFailedEvent event = new EmailDeliveryFailedEvent(UUID.randomUUID(), "boom", null);

            assertThat(event.requestId()).isEqualTo("the-open-request-id");
        }
    }

    @Test
    void aNullRequestIdStaysNullOutsideAnyOpenUnitOfWork() {
        EmailDeliveryFailedEvent event = new EmailDeliveryFailedEvent(UUID.randomUUID(), "boom", null);

        assertThat(event.requestId()).isNull();
    }

    @Test
    void anExplicitRequestIdIsKept() {
        EmailDeliveryFailedEvent event =
                new EmailDeliveryFailedEvent(UUID.randomUUID(), "boom", "explicit-request-id");

        assertThat(event.requestId()).isEqualTo("explicit-request-id");
    }

    @Test
    void theTwoArgConstructorDefaultsRequestIdTheSameWay() {
        UUID correlationId = UUID.randomUUID();

        try (LogContext.Scope ignored = LogContext.open("the-open-request-id")) {
            EmailDeliveryFailedEvent event = new EmailDeliveryFailedEvent(correlationId, "boom");

            assertThat(event.correlationId()).isEqualTo(correlationId);
            assertThat(event.reason()).isEqualTo("boom");
            assertThat(event.requestId()).isEqualTo("the-open-request-id");
        }
    }
}
