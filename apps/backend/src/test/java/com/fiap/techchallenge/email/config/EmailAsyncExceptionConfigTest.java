package com.fiap.techchallenge.email.config;

import com.fiap.techchallenge.email.exceptions.EmailExpiredException;
import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatCode;

class EmailAsyncExceptionConfigTest {

    private final AsyncUncaughtExceptionHandler handler =
            new EmailAsyncExceptionConfig().getAsyncUncaughtExceptionHandler();

    @Test
    void anExpiredEmailIsHandledWithoutPropagating() throws NoSuchMethodException {
        Method method = EmailAsyncExceptionConfigTest.class.getDeclaredMethod("dummy");

        assertThatCode(() -> handler.handleUncaughtException(
                new EmailExpiredException("expired"), method))
                .doesNotThrowAnyException();
    }

    @Test
    void anyOtherFailureIsHandledWithoutPropagating() throws NoSuchMethodException {
        Method method = EmailAsyncExceptionConfigTest.class.getDeclaredMethod("dummy");

        assertThatCode(() -> handler.handleUncaughtException(
                new IllegalStateException("boom"), method))
                .doesNotThrowAnyException();
    }

    private void dummy() {
    }
}
