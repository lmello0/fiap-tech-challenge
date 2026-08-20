package com.fiap.techchallenge.auth.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.shared.exceptions.ProblemDetails;
import com.fiap.techchallenge.shared.logging.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;

/**
 * Holds a flagged account to the one thing it is allowed to do: rotate the password someone else
 * chose for it. Runs once the bearer token has been authenticated, and reads
 * {@link JwtConfig#PASSWORD_CHANGE_CLAIM} off it.
 *
 * <p>This is a filter rather than a rule in {@code authorizeHttpRequests} because a denial there
 * throws {@code AccessDeniedException} inside the filter chain, where no {@code @RestControllerAdvice}
 * can see it — the caller would get a bodyless 403 indistinguishable from an ordinary permission
 * failure. Writing the problem detail here is what gives a client a title it can branch on.
 */
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    /**
     * Its own, rather than the web layer's bean: the security chain is built before Jackson's
     * autoconfiguration is visible to it. Nulls are dropped so this body is shaped like every other
     * error in the app — {@code ProblemDetail} leaves {@code type} and {@code properties} unset here,
     * and the advice-rendered bodies do not carry them either.
     */
    private static final ObjectMapper JSON = new ObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    private final RequestMatcher allowed;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        if (!passwordChangeRequired() || allowed.matches(request)) {
            chain.doFilter(request, response);
            return;
        }

        LogContext.put("outcome", "password_change_required");

        ProblemDetail problem = ProblemDetails.of(
                HttpStatus.FORBIDDEN,
                "Password change required",
                "You must change your password before using the application"
        );
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        JSON.writeValue(response.getWriter(), problem);
    }

    /**
     * False for anything that arrives without an authenticated JWT — the permitted routes (login,
     * registration, the token confirmations) carry no token at all, and this filter has no opinion
     * about them.
     */
    private boolean passwordChangeRequired() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.getPrincipal() instanceof Jwt jwt
                && Boolean.TRUE.equals(jwt.getClaimAsBoolean(JwtConfig.PASSWORD_CHANGE_CLAIM));
    }
}
