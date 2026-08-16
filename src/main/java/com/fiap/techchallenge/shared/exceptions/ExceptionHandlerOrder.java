package com.fiap.techchallenge.shared.exceptions;

import org.springframework.core.Ordered;

/**
 * Precedence for the {@code @RestControllerAdvice} beans. Spring consults advices in ascending order,
 * so a lower value wins.
 */
public final class ExceptionHandlerOrder {

    /**
     * Module advices are consulted first, so a module's own mapping always wins over the catch-all.
     */
    public static final int MODULE = Ordered.HIGHEST_PRECEDENCE + 10;

    /**
     * The catch-all runs last; anything a module claimed never reaches it.
     */
    public static final int GLOBAL = Ordered.LOWEST_PRECEDENCE;

    private ExceptionHandlerOrder() {
    }
}
