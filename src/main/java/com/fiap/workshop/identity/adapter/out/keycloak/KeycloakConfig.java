package com.fiap.workshop.identity.adapter.out.keycloak;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KeycloakProperties.class)
class KeycloakConfig {
}
