package com.fiap.workshop.identity.adapter.out.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("workshop.keycloak")
record KeycloakProperties(String baseUrl, String realm, String adminClientId, String adminClientSecret) {
}
