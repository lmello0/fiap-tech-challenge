package com.fiap.techchallenge.auth.api.representation;

import java.time.Duration;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        Duration expiresIn
) {
}
