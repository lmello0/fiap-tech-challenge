package com.fiap.workshop.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;

@Configuration
class OpenApiConfig {

	private static final String SCHEME = "keycloak";
	private static final String PROBLEM = "ProblemDetail";
	private static final Pattern ROLE = Pattern.compile("'([A-Z_]+)'");

	// Operations whose body points at other resources (work order number, service or part ids) that may not exist.
	private static final Set<String> REFERENCES_IN_BODY = Set.of(
			"POST /api/v1/work-orders",
			"POST /api/v1/webhooks/budget-decisions");

	@Bean
	OpenAPI openApi(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
		return new OpenAPI()
				.info(new Info().title("Workshop API").version("v1")
						.description("Auto repair shop: customers, vehicles, catalog, inventory and work orders."))
				// Relative, so the document is the same whichever host serves it (and the versioned copy stays stable).
				.servers(List.of(new Server().url("/").description("The host serving this document")))
				.components(new Components().addSecuritySchemes(SCHEME, new SecurityScheme()
						.type(SecurityScheme.Type.OAUTH2)
						.flows(new OAuthFlows().authorizationCode(new OAuthFlow()
								.authorizationUrl(issuer + "/protocol/openid-connect/auth")
								.tokenUrl(issuer + "/protocol/openid-connect/token")
								.refreshUrl(issuer + "/protocol/openid-connect/token")
								.scopes(new Scopes().addString("openid", "OpenID Connect"))))))
				.addSecurityItem(new SecurityRequirement().addList(SCHEME));
	}

	/** Appends the roles from {@code @PreAuthorize} (method or class level) to each operation's description. */
	@Bean
	OperationCustomizer requiredRoles() {
		return (operation, handlerMethod) -> {
			PreAuthorize rule = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), PreAuthorize.class);
			if (rule == null) {
				rule = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
			}
			if (rule == null) {
				return operation;
			}
			List<String> roles = ROLE.matcher(rule.value()).results().map(m -> m.group(1)).toList();
			String line = "Required role: " + String.join(" or ", roles) + ".";
			operation.setDescription(operation.getDescription() == null ? line : operation.getDescription() + "\n\n" + line);
			return operation;
		};
	}

	/** Documents the ProblemDetail errors produced by GlobalExceptionHandler, Spring MVC and the security filters. */
	@Bean
	OpenApiCustomizer errorResponses() {
		return openApi -> {
			openApi.getComponents().addSchemas(PROBLEM, problemSchema());
			openApi.getPaths().forEach((path, item) -> item.readOperationsMap()
					.forEach((method, operation) -> addErrors(path, method, operation)));
		};
	}

	private static void addErrors(String path, PathItem.HttpMethod method, Operation operation) {
		boolean isPublic = operation.getSecurity() != null && operation.getSecurity().isEmpty();
		boolean isRead = method == PathItem.HttpMethod.GET;
		boolean hasInput = operation.getRequestBody() != null
				|| (operation.getParameters() != null && !operation.getParameters().isEmpty());
		ApiResponses responses = operation.getResponses();

		if (hasInput) {
			error(responses, "400", "Invalid request: a field, parameter or path variable failed validation");
		}
		error(responses, "401", isPublic ? "Missing, invalid or expired webhook signature"
				: "Missing, invalid or expired bearer token");
		if (!isPublic) {
			error(responses, "403", "The token does not carry a role allowed to call this operation");
		}
		if (path.contains("{") || path.startsWith("/api/v1/me") || REFERENCES_IN_BODY.contains(method + " " + path)) {
			error(responses, "404", "A referenced resource does not exist (or is not visible to the caller)");
		}
		if (!isRead) {
			error(responses, "409", "Conflicts with the current state: duplicate, invalid status transition or concurrent update");
			error(responses, "422", "Violates a business rule");
		}
	}

	private static void error(ApiResponses responses, String status, String description) {
		responses.putIfAbsent(status, new ApiResponse().description(description).content(new Content()
				.addMediaType("application/problem+json", new MediaType().schema(new Schema<>().$ref(PROBLEM)))));
	}

	private static Schema<?> problemSchema() {
		return new ObjectSchema().description("RFC 9457 problem details")
				.addProperty("type", new StringSchema().format("uri"))
				.addProperty("title", new StringSchema())
				.addProperty("status", new IntegerSchema())
				.addProperty("detail", new StringSchema())
				.addProperty("instance", new StringSchema().format("uri"));
	}
}
