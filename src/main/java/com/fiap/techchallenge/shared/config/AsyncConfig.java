package com.fiap.techchallenge.shared.config;

import com.fiap.techchallenge.email.exceptions.EmailExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Modulith enables async execution on its own; this declares it explicitly so we can attach an
 * uncaught exception handler (declaring an {@code AsyncConfigurer} makes Modulith's
 * {@code AsyncEnablingConfiguration} back off, which is the designed hand-off).
 *
 * <p>An expired email is an expected outcome rather than a fault, so it is reported as a single
 * warning instead of a stack trace — it still propagates out of the listener, which is what leaves the
 * publication uncompleted (see {@link EmailExpiredException}).
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            if (throwable instanceof EmailExpiredException) {
                log.warn("{}", throwable.getMessage());
                return;
            }

            log.error("Async execution of {} failed", method.getName(), throwable);
        };
    }
}
