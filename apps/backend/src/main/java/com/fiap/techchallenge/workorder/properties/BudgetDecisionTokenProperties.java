package com.fiap.techchallenge.workorder.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The HMAC signing key behind {@code BudgetDecisionTokenService} (ADR 0021) — a dedicated secret,
 * never shared with the JWT signing key, so a leak of one never weakens the other. */
@ConfigurationProperties(prefix = "app.workorder.budget-decision-token")
public record BudgetDecisionTokenProperties(
        String secret
) {
}
