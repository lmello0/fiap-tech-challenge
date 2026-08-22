package com.fiap.techchallenge.auth.entities;

import com.fiap.techchallenge.auth.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserAuthId implements Serializable {

    private UUID userId;
    private AuthProvider provider;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAuthId that)) return false;

        return Objects.equals(userId, that.userId) && provider == that.provider;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, provider);
    }
}
