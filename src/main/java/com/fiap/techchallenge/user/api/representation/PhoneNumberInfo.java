package com.fiap.techchallenge.user.api.representation;

import com.fiap.techchallenge.user.enums.PhoneType;

import java.time.Instant;

public record PhoneNumberInfo(
        PhoneType type,
        String phone,
        boolean isPrimary,
        Instant createdAt
) {
}
