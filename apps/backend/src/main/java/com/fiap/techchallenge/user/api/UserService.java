package com.fiap.techchallenge.user.api;

import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import com.fiap.techchallenge.user.api.commands.CreateWorkerCommand;
import com.fiap.techchallenge.user.api.commands.UpdateUserProfileCommand;
import com.fiap.techchallenge.user.api.queries.UserFilterQuery;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.user.api.representation.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    UserInfo createCustomer(CreateUserCommand command);

    UserInfo createWorker(CreateWorkerCommand command);

    void terminateWorker(UUID userId, LocalDate on);

    UserInfo updateProfile(UUID userId, UpdateUserProfileCommand command);

    void deactivateCustomer(UUID userId);

    void reactivateCustomer(UUID userId);

    void markEmailVerified(UUID userId);

    void changeEmail(UUID userId, String newEmail);

    Page<UserInfo> listCustomers(UserFilterQuery filter, Pageable pageable);

    Page<UserInfo> listWorkers(UserFilterQuery filter, Pageable pageable);

    UserInfo getById(UUID id);

    Optional<UserInfo> findById(UUID id);

    Optional<UserInfo> findByEmail(String email);

    Optional<UserPrincipal> findPrincipalById(UUID id);

    Optional<UserPrincipal> findPrincipalByEmail(String email);
}
