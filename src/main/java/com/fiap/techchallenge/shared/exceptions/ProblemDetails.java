package com.fiap.techchallenge.shared.exceptions;

import com.fiap.techchallenge.shared.logging.LogContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Single factory for RFC 7807 bodies, so every module's advice answers in the same shape.
 */
public final class ProblemDetails {

    /**
     * Every error body carries {@code requestId} (ADR 0017), not just the response header — a
     * caller reading the JSON body directly still has something to hand support.
     */
    public static ProblemDetail of(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);

        String requestId = LogContext.requestId();
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }

        return problem;
    }

    private ProblemDetails() {
    }
}
