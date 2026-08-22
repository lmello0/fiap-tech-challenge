package com.fiap.techchallenge.shared.logging;

import org.slf4j.MDC;

/**
 * The {@code requestId} every log line carries, and the accumulator behind each unit of work's
 * canonical line (ADR 0017). {@link #put} doesn't log anything by itself — it just leaves a field
 * in {@link MDC} for whichever single line closes out the request, event-listener invocation, or
 * scheduled job run currently open, so a handler three calls deep can contribute to that line
 * without holding a reference to it.
 */
public final class LogContext {

    private static final String REQUEST_ID_KEY = "requestId";

    /** {@code null} outside any unit of work opened by {@link #open}. */
    public static String requestId() {
        return MDC.get(REQUEST_ID_KEY);
    }

    /**
     * A no-op outside any {@link #open} scope — otherwise a call site invoked from code nobody
     * wrapped would leave a field with no {@link Scope#close()} to ever clear it, silently leaking
     * into whatever unrelated unit of work reuses this thread next.
     */
    public static void put(String key, Object value) {
        if (value != null && requestId() != null) {
            MDC.put(key, String.valueOf(value));
        }
    }

    /**
     * Opens a unit of work under {@code requestId}. Closing always clears the whole MDC, not just
     * {@code requestId} — thread-pool threads are reused across unrelated units of work, so a field
     * left behind here would silently leak into whatever runs next on the same thread.
     */
    public static Scope open(String requestId) {
        MDC.put(REQUEST_ID_KEY, requestId);
        return MDC::clear;
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    private LogContext() {
    }
}
