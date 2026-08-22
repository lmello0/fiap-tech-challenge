package com.fiap.techchallenge.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Opens {@link LogContext} for every request and closes it with exactly one canonical log line
 * (ADR 0017) — {@code method}, {@code path}, {@code status}, {@code durationMs}, plus whatever
 * fields the request handling put into context along the way. Registered directly with the
 * servlet container ahead of Spring Security's own filter chain (see {@link RequestLoggingConfig}),
 * so an authentication failure still gets a {@code requestId} and a canonical line, not just
 * requests that reach a controller.
 *
 * <p>Reads {@code X-Request-Id} from the caller if present, otherwise generates one — either way
 * it's echoed back on the response, so a caller always has something to hand support.
 */
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long startNanos = System.nanoTime();

        try (LogContext.Scope ignored = LogContext.open(requestId)) {
            try {
                chain.doFilter(request, response);
            } finally {
                LogContext.put("method", request.getMethod());
                LogContext.put("path", request.getRequestURI());
                LogContext.put("status", response.getStatus());
                LogContext.put("durationMs", (System.nanoTime() - startNanos) / 1_000_000);

                if (response.getStatus() >= 500) {
                    log.error("http_request");
                } else {
                    log.info("http_request");
                }
            }
        }
    }

    private static String resolveRequestId(HttpServletRequest request) {
        String header = request.getHeader(REQUEST_ID_HEADER);

        return (header == null || header.isBlank()) ? UUID.randomUUID().toString() : header;
    }
}
