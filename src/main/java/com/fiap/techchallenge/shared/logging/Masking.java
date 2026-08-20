package com.fiap.techchallenge.shared.logging;

/**
 * The one place PII is allowed to touch a log line (ADR 0017): a call site masks before handing a
 * value to {@link LogContext#put} or a log statement, rather than trusting review to catch a raw
 * value that shouldn't be there.
 */
public final class Masking {

    /** {@code "jd***@example.com"} — keeps the domain (useful for grepping by tenant/provider). */
    public static String email(String email) {
        if (email == null) {
            return null;
        }

        int at = email.indexOf('@');
        if (at <= 2) {
            return "***" + email.substring(Math.max(at, 0));
        }

        return email.substring(0, 2) + "***" + email.substring(at);
    }

    private Masking() {
    }
}
