package com.fiap.workshop.shared.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Maps the Keycloak {@code realm_access.roles} claim to {@code ROLE_*} authorities. */
public class KeycloakRealmRolesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		return new JwtAuthenticationToken(jwt, authorities(jwt), jwt.getSubject());
	}

	private static Collection<GrantedAuthority> authorities(Jwt jwt) {
		Object realmAccess = jwt.getClaim("realm_access");
		if (!(realmAccess instanceof Map<?, ?> map) || !(map.get("roles") instanceof Collection<?> roles)) {
			return List.of();
		}
		return roles.stream()
				.map(String::valueOf)
				.map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
				.toList();
	}
}
