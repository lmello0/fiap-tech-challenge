package com.fiap.techchallenge.auth.services;

import com.fiap.techchallenge.auth.config.JwtConfig;
import com.fiap.techchallenge.auth.properties.JwtProperties;
import com.fiap.techchallenge.user.api.representation.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;


@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public String issueAccessToken(UserPrincipal user, List<String> roles, boolean needPasswordChange) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTTL()))
                .subject(user.id().toString())
                .claim(JwtConfig.TOKEN_TYPE_CLAIM, JwtConfig.ACCESS_TOKEN_TYPE)
                .claim("email", user.email())
                .claim("roles", List.copyOf(roles))
                // Always stamped, true or false: an absent claim and a false one would otherwise be
                // the same thing at the filter, and "absent" is what a forged token looks like.
                .claim(JwtConfig.PASSWORD_CHANGE_CLAIM, needPasswordChange)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return encoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    public Duration accessTokenTTL() {
        return properties.accessTokenTTL();
    }
}
