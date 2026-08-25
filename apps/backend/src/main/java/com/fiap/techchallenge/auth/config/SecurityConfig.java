package com.fiap.techchallenge.auth.config;

import com.fiap.techchallenge.auth.properties.CorsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * All an account pending a forced password change may still reach: rotating the password, and
     * getting out. Everything else answers 403 until the rotation lands — see
     * {@link PasswordChangeRequiredFilter}.
     */
    private static final RequestMatcher PASSWORD_CHANGE_ALLOWLIST = new OrRequestMatcher(
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/auth/password/change"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/auth/logout"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/auth/logout-all")
    );

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            CorsConfigurationSource corsConfigurationSource
    ) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/auth/register/customer",
                                "/auth/login",
                                "/auth/refresh-token",
                                "/auth/logout",
                                "/auth/password/reset",
                                "/auth/password/reset/confirm",
                                "/auth/email-verification/resend",
                                "/auth/email-verification/confirm",
                                "/auth/email-change/confirm").permitAll()
                        // Guest scheduling (CONTEXT.md "Guest"): booking a Drop-off and every
                        // token-driven guest action require no account — see ADR 0015.
                        .requestMatchers(HttpMethod.POST,
                                "/appointments/dropoff/guest",
                                "/appointments/pickup/book",
                                "/appointments/guest/view",
                                "/appointments/guest/cancel",
                                "/appointments/guest/reschedule",
                                "/appointments/guest/complete-registration").permitAll()
                        .requestMatchers(HttpMethod.GET, "/appointments/availability").permitAll()
                        // Budget Decision Token (CONTEXT.md): a customer acts on a sent Budget straight
                        // from the emailed link, no account needed — possession of the token is the
                        // credential (ADR 0021).
                        .requestMatchers(HttpMethod.POST,
                                "/budgets/decision/view",
                                "/budgets/decision/approval",
                                "/budgets/decision/refusal").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                // After the bearer filter, so the JWT it reads is already authenticated.
                .addFilterAfter(
                        new PasswordChangeRequiredFilter(PASSWORD_CHANGE_ALLOWLIST),
                        BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        converter.setPrincipalClaimName("sub");

        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();

        List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        List<String> allowedHeaders = List.of("*");
        boolean allowCredentials = true;

        config.setAllowedOrigins(properties.allowedOrigins());
        config.setAllowedMethods(allowedMethods);
        config.setAllowedHeaders(allowedHeaders);
        config.setAllowCredentials(allowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("CORS configured for allowedOrigins={} allowedMethods={} allowedHeaders={} allowCredentials={}",
                properties.allowedOrigins(),
                allowedMethods,
                allowedHeaders,
                allowCredentials
        );

        return source;
    }
}
