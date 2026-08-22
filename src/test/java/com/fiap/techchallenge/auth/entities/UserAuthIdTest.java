package com.fiap.techchallenge.auth.entities;

import com.fiap.techchallenge.auth.enums.AuthProvider;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserAuthIdTest {

    private final UUID userId = UUID.randomUUID();

    @Test
    void isEqualToItself() {
        UserAuthId id = new UserAuthId(userId, AuthProvider.LOCAL);

        assertThat(id).isEqualTo(id);
    }

    @Test
    void isEqualToAnotherInstanceWithTheSameUserIdAndProvider() {
        UserAuthId first = new UserAuthId(userId, AuthProvider.LOCAL);
        UserAuthId second = new UserAuthId(userId, AuthProvider.LOCAL);

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void isNotEqualWhenTheUserIdDiffers() {
        UserAuthId first = new UserAuthId(userId, AuthProvider.LOCAL);
        UserAuthId second = new UserAuthId(UUID.randomUUID(), AuthProvider.LOCAL);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void isNotEqualToNullOrAnUnrelatedType() {
        UserAuthId id = new UserAuthId(userId, AuthProvider.LOCAL);

        assertThat(id).isNotEqualTo(null);
        assertThat(id).isNotEqualTo("not a UserAuthId");
    }
}
