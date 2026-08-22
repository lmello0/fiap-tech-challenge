package com.fiap.techchallenge.shared.logging;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link RequestIdFilter} with the servlet container directly, ahead of Spring
 * Security's filter chain — {@code addFilterBefore} on {@code HttpSecurity} would only place it
 * inside that chain, still after whatever the container runs first, and a request Security turns
 * away before authentication wouldn't get a {@code requestId} or a canonical line.
 */
@Configuration
public class RequestLoggingConfig {

    @Bean
    FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(new RequestIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }
}
