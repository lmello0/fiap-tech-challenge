package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.workorder.exceptions.InvalidBudgetTokenException;
import com.fiap.techchallenge.workorder.properties.BudgetDecisionTokenProperties;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetDecisionTokenServiceTest {

    private final BudgetDecisionTokenService service =
            new BudgetDecisionTokenService(new BudgetDecisionTokenProperties("test-secret-at-least-32-bytes-long!!"));

    @Test
    void anIssuedTokenResolvesBackToItsBudgetId() {
        UUID budgetId = UUID.randomUUID();

        String token = service.issue(budgetId);

        assertThat(service.resolve(token)).isEqualTo(budgetId);
    }

    @Test
    void issuingTheSameBudgetIdTwiceProducesTheIdenticalToken() {
        UUID budgetId = UUID.randomUUID();

        assertThat(service.issue(budgetId)).isEqualTo(service.issue(budgetId));
    }

    @Test
    void rejectsATokenWithATamperedSignature() {
        UUID budgetId = UUID.randomUUID();
        String token = service.issue(budgetId);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> service.resolve(tampered))
                .isInstanceOf(InvalidBudgetTokenException.class);
    }

    @Test
    void rejectsATokenSignedForADifferentBudget() {
        String tokenForOtherBudget = service.issue(UUID.randomUUID());
        String forgedToken = UUID.randomUUID() + tokenForOtherBudget.substring(tokenForOtherBudget.indexOf('.'));

        assertThatThrownBy(() -> service.resolve(forgedToken))
                .isInstanceOf(InvalidBudgetTokenException.class);
    }

    @Test
    void rejectsGarbageInput() {
        assertThatThrownBy(() -> service.resolve("not-a-real-token"))
                .isInstanceOf(InvalidBudgetTokenException.class);

        assertThatThrownBy(() -> service.resolve("not-a-uuid.c2lnbmF0dXJl"))
                .isInstanceOf(InvalidBudgetTokenException.class);
    }
}
