package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.workorder.exceptions.InvalidBudgetTokenException;
import com.fiap.techchallenge.workorder.properties.BudgetDecisionTokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * The Budget Decision Token (CONTEXT.md) is a stateless HMAC-signed capability — {@code budgetId}
 * plus a signature over it — not a persisted opaque token like every other token in this codebase
 * (ADR 0021). That statelessness is what makes "resend re-mails the same link" (ADR 0021) trivially
 * true: the same budgetId always signs to the same token, with nothing to look up or expire on a
 * clock. Validity instead rides entirely on the Budget's own state at the moment the link is used —
 * {@link BudgetServiceImpl}'s token-driven approve/refuse go through the same
 * {@code budgetStateMachine} transition the authenticated path does, so a resolved Budget rejects a
 * repeat decision exactly like it already would for a signed-in customer.
 */
@Service
@RequiredArgsConstructor
public class BudgetDecisionTokenService {

    private static final String ALGORITHM = "HmacSHA256";

    private final BudgetDecisionTokenProperties properties;

    public String issue(UUID budgetId) {
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sign(budgetId));
        return budgetId + "." + signature;
    }

    public UUID resolve(String rawToken) {
        int dot = rawToken == null ? -1 : rawToken.indexOf('.');
        if (dot < 0) {
            throw new InvalidBudgetTokenException();
        }

        UUID budgetId;
        byte[] signature;
        try {
            budgetId = UUID.fromString(rawToken.substring(0, dot));
            signature = Base64.getUrlDecoder().decode(rawToken.substring(dot + 1));
        } catch (IllegalArgumentException e) {
            throw new InvalidBudgetTokenException();
        }

        if (!MessageDigest.isEqual(signature, sign(budgetId))) {
            throw new InvalidBudgetTokenException();
        }

        return budgetId;
    }

    private byte[] sign(UUID budgetId) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(budgetId.toString().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
