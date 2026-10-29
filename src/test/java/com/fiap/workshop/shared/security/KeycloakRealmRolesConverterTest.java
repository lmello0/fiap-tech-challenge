package com.fiap.workshop.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRealmRolesConverterTest {

	private final KeycloakRealmRolesConverter converter = new KeycloakRealmRolesConverter();

	private static Jwt.Builder jwt() {
		return Jwt.withTokenValue("t").header("alg", "none").subject("user-1");
	}

	@Test
	void mapsRealmRolesToRoleAuthorities() {
		Jwt jwt = jwt().claim("realm_access", Map.of("roles", List.of("ADMIN", "MECHANIC"))).build();

		assertThat(converter.convert(jwt).getAuthorities().stream().map(GrantedAuthority::getAuthority))
				.containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MECHANIC");
	}

	@Test
	void hasNoAuthoritiesWithoutRealmAccess() {
		assertThat(converter.convert(jwt().claim("scope", "openid").build()).getAuthorities()).isEmpty();
	}

	@Test
	void ignoresMalformedRealmAccess() {
		assertThat(converter.convert(jwt().claim("realm_access", Map.of("roles", "ADMIN")).build()).getAuthorities()).isEmpty();
	}

	@Test
	void usesSubjectAsPrincipalName() {
		assertThat(converter.convert(jwt().claim("k", "v").build()).getName()).isEqualTo("user-1");
	}
}
