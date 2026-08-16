package com.fiap.techchallenge.user.api.queries;

public record UserFilterQuery(
        String name,
        String email,
        String document,
        String phone
) {
}
