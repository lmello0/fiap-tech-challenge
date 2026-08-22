package com.fiap.techchallenge.auth.repositories;

import com.fiap.techchallenge.auth.entities.UserAuth;
import com.fiap.techchallenge.auth.entities.UserAuthId;
import com.fiap.techchallenge.auth.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthRepository extends JpaRepository<UserAuth, UserAuthId> {

    Optional<UserAuth> findByUserIdAndProvider(UUID userId, AuthProvider provider);

}
