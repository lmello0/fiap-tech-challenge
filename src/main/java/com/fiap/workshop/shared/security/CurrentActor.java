package com.fiap.workshop.shared.security;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Who is calling: the JWT subject, its roles and, for customers, the linked {@code customer_id} claim. */
@Component
public class CurrentActor {

	public record Actor(String subject, Set<Role> roles, UUID customerId) {

		public boolean has(Role role) {
			return roles.contains(role);
		}
	}

	public Actor get() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new IllegalStateException("No authenticated JWT in the security context");
		}
		Set<Role> roles = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.map(CurrentActor::toRole)
				.flatMap(Optional::stream)
				.collect(Collectors.toSet());
		String customerId = jwt.getClaimAsString("customer_id");
		return new Actor(jwt.getSubject(), roles, customerId == null ? null : UUID.fromString(customerId));
	}

	private static Optional<Role> toRole(String authority) {
		for (Role role : Role.values()) {
			if (role.authority().equals(authority)) {
				return Optional.of(role);
			}
		}
		return Optional.empty();
	}
}
