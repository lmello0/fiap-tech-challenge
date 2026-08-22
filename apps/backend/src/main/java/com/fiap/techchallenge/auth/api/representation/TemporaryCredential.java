package com.fiap.techchallenge.auth.api.representation;

import com.fiap.techchallenge.user.api.representation.UserInfo;

/**
 * The result of staff-initiated customer registration: nobody but the system knows
 * {@code rawPassword}, since the customer never chose it — the caller (an Attendant standing with
 * the customer) is expected to relay it verbally.
 */
public record TemporaryCredential(
        UserInfo user,
        String rawPassword
) {
}
