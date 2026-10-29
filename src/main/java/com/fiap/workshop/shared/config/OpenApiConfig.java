package com.fiap.workshop.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

	private static final String SCHEME = "keycloak";

	@Bean
	OpenAPI openApi(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
		return new OpenAPI()
				.info(new Info().title("Workshop API").version("v1")
						.description("Auto repair shop: customers, vehicles, catalog, inventory and work orders."))
				.components(new Components().addSecuritySchemes(SCHEME, new SecurityScheme()
						.type(SecurityScheme.Type.OAUTH2)
						.flows(new OAuthFlows().authorizationCode(new OAuthFlow()
								.authorizationUrl(issuer + "/protocol/openid-connect/auth")
								.tokenUrl(issuer + "/protocol/openid-connect/token")
								.refreshUrl(issuer + "/protocol/openid-connect/token")
								.scopes(new Scopes().addString("openid", "OpenID Connect"))))))
				.addSecurityItem(new SecurityRequirement().addList(SCHEME));
	}
}
