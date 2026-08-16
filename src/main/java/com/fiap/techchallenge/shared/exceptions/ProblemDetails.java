package com.fiap.techchallenge.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Single factory for RFC 7807 bodies, so every module's advice answers in the same shape.
 */
public final class ProblemDetails {

    public static ProblemDetail of(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);

        return problem;
    }

    private ProblemDetails() {
    }
}
