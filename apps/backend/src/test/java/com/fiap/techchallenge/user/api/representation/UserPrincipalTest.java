package com.fiap.techchallenge.user.api.representation;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** enabled() is (customer && customerActive) || (worker && workerActive) — all four combinations. */
class UserPrincipalTest {

    @Test
    void anActiveCustomerOnlyIsEnabled() {
        assertThat(principal(true, true, false, false).enabled()).isTrue();
    }

    @Test
    void anActiveWorkerOnlyIsEnabled() {
        assertThat(principal(false, false, true, true).enabled()).isTrue();
    }

    @Test
    void aDeactivatedCustomerWithNoWorkerFacetIsDisabled() {
        assertThat(principal(true, false, false, false).enabled()).isFalse();
    }

    @Test
    void aTerminatedWorkerWithNoCustomerFacetIsDisabled() {
        assertThat(principal(false, false, true, false).enabled()).isFalse();
    }

    @Test
    void neitherFacetPresentIsDisabled() {
        assertThat(principal(false, false, false, false).enabled()).isFalse();
    }

    private UserPrincipal principal(boolean customer, boolean customerActive, boolean worker, boolean workerActive) {
        return new UserPrincipal(UUID.randomUUID(), "a@example.com", true, customer, customerActive, worker, workerActive, null);
    }
}
