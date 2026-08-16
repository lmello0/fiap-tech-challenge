package com.fiap.techchallenge.auth.services;

import com.fiap.techchallenge.auth.api.AuthService;
import com.fiap.techchallenge.auth.api.commands.ChangePasswordCommand;
import com.fiap.techchallenge.auth.api.commands.ConfirmEmailChangeCommand;
import com.fiap.techchallenge.auth.api.commands.ConfirmEmailVerificationCommand;
import com.fiap.techchallenge.auth.api.commands.ConfirmPasswordResetCommand;
import com.fiap.techchallenge.auth.api.commands.LoginCommand;
import com.fiap.techchallenge.auth.api.commands.RegisterCustomerCommand;
import com.fiap.techchallenge.auth.api.commands.RegisterWorkerCommand;
import com.fiap.techchallenge.auth.api.commands.RequestEmailChangeCommand;
import com.fiap.techchallenge.auth.api.commands.RequestPasswordResetCommand;
import com.fiap.techchallenge.auth.api.commands.ResendEmailVerificationCommand;
import com.fiap.techchallenge.auth.api.representation.TokenResponse;
import com.fiap.techchallenge.auth.entities.UserAuth;
import com.fiap.techchallenge.auth.enums.AuthProvider;
import com.fiap.techchallenge.auth.exceptions.AccountDisabledException;
import com.fiap.techchallenge.auth.exceptions.AccountLockedException;
import com.fiap.techchallenge.auth.exceptions.EmailNotVerifiedException;
import com.fiap.techchallenge.auth.exceptions.InvalidTokenException;
import com.fiap.techchallenge.auth.exceptions.RegistrationConflictException;
import com.fiap.techchallenge.auth.properties.LoginRateLimitProperties;
import com.fiap.techchallenge.auth.repositories.UserAuthRepository;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.user.api.representation.UserPrincipal;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAuthRepository userAuthRepository;

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final EmailChangeService emailChangeService;
    private final LoginRateLimitProperties rateLimitProperties;

    private String dummyHash;

    @PostConstruct
    void init() {
        this.dummyHash = passwordEncoder.encode("timing-attack-mitigation-placeholder");
    }

    @Override
    @Transactional
    public TokenResponse registerCustomer(RegisterCustomerCommand command) {
        try {
            UserInfo user = userService.createCustomer(command.user());

            userAuthRepository.save(UserAuth.local(user.id(), passwordEncoder.encode(command.rawPassword())));
            emailVerificationService.issueEmailVerification(user.id(), user.email());

            log.info("Local registration succeeded userId={}", user.id());

            return issueFor(principalOf(user.id()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Registration conflict (race on a unique index)");
            throw new RegistrationConflictException();
        }
    }

    @Override
    @Transactional
    public UserInfo registerWorker(RegisterWorkerCommand command) {
        try {
            UserInfo user = userService.createWorker(command.worker());

            userAuthRepository.save(UserAuth.local(user.id(), passwordEncoder.encode(command.rawPassword())));
            emailVerificationService.issueEmailVerification(user.id(), user.email());

            log.info("Worker registration succeeded userId={}", user.id());

            return user;
        } catch (DataIntegrityViolationException e) {
            log.warn("Worker registration conflict (race on a unique index)");
            throw new RegistrationConflictException();
        }
    }

    @Override
    @Transactional
    public TokenResponse login(LoginCommand command) {
        UserPrincipal user = userService
                .findPrincipalByEmail(command.email())
                .orElse(null);

        Optional<UserAuth> credential = user == null
                ? Optional.empty()
                : userAuthRepository.findByUserIdAndProvider(user.id(), AuthProvider.LOCAL);

        Instant now = Instant.now();
        if (credential.isPresent() && credential.get().isLocked(now)) {
            log.warn("Login blocked (account locked) userId={}", user.id());
            throw new AccountLockedException("Account is temporarily locked due to too many failed attempts");
        }

        String hashToCheck = credential
                .map(UserAuth::getPasswordHash)
                .orElse(dummyHash);

        boolean passwordOk = passwordEncoder.matches(command.rawPassword(), hashToCheck);

        if (user == null || !passwordOk) {
            credential.ifPresent(c -> c.registerFailedAttempt(now, rateLimitProperties.maxAttempts(), rateLimitProperties.lockDuration()));
            log.warn("Login failed (invalid credentials)");
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.emailVerified()) {
            log.warn("Login blocked (email not verified) userId={}", user.id());
            throw new EmailNotVerifiedException("Email address has not been verified yet");
        }

        ensureActive(user);
        credential.ifPresent(UserAuth::resetFailedAttempts);

        log.info("Login succeeded userId={}", user.id());
        return issueFor(user);
    }

    @Override
    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);

        UserPrincipal user = userService.findPrincipalById(rotation.userId())
                .orElseThrow(() -> new InvalidTokenException("Unknown token subject"));

        ensureActive(user);

        return new TokenResponse(
                jwtService.issueAccessToken(user, authorities(user)),
                rotation.newToken(),
                jwtService.accessTokenTTL()
        );
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Override
    @Transactional
    public void logoutEverywhere(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
        log.info("Logged out everywhere userId={}", userId);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordCommand command) {
        UserAuth credential = userAuthRepository
                .findByUserIdAndProvider(userId, AuthProvider.LOCAL)
                .filter(auth -> auth.getPasswordHash() != null)
                .orElse(null);

        boolean currentPasswordOk = credential != null
                && passwordEncoder.matches(command.currentPassword(), credential.getPasswordHash());

        if (!currentPasswordOk) {
            log.warn("Password change rejected (invalid current password) userId={}", userId);
            throw new BadCredentialsException("Invalid credentials");
        }

        credential.changePassword(passwordEncoder.encode(command.newPassword()));
        refreshTokenService.revokeAllForUser(userId);

        log.info("Password changed userId={}", userId);
    }

    @Override
    public void requestPasswordReset(RequestPasswordResetCommand command) {
        passwordResetService.requestPasswordReset(command.email());
    }

    @Override
    public void confirmPasswordReset(ConfirmPasswordResetCommand command) {
        passwordResetService.confirmPasswordReset(command.token(), command.newPassword());
    }

    @Override
    public void confirmEmailVerification(ConfirmEmailVerificationCommand command) {
        emailVerificationService.confirmEmailVerification(command.token());
    }

    @Override
    public void resendEmailVerification(ResendEmailVerificationCommand command) {
        emailVerificationService.resendEmailVerification(command.email());
    }

    @Override
    public void requestEmailChange(UUID userId, RequestEmailChangeCommand command) {
        emailChangeService.requestEmailChange(userId, command.newEmail());
    }

    @Override
    public void confirmEmailChange(ConfirmEmailChangeCommand command) {
        emailChangeService.confirmEmailChange(command.token());
    }

    private TokenResponse issueFor(UserPrincipal user) {
        return new TokenResponse(
                jwtService.issueAccessToken(user, authorities(user)),
                refreshTokenService.issue(user.id()),
                jwtService.accessTokenTTL()
        );
    }

    private void ensureActive(UserPrincipal user) {
        if (!user.enabled()) {
            log.warn("Login blocked for disabled account userId={}", user.id());
            throw new AccountDisabledException("Account is disabled");
        }
    }

    private List<String> authorities(UserPrincipal user) {
        List<String> roles = new ArrayList<>();

        if (user.customer() && user.customerActive()) {
            roles.add("CUSTOMER");
        }

        if (user.worker() && user.workerActive()) {
            roles.add("WORKER");
            roles.add(user.workerRole().name());
        }

        return roles;
    }

    private UserPrincipal principalOf(UUID userId) {
        return userService.findPrincipalById(userId)
                .orElseThrow(() -> new IllegalStateException("User vanished right after creation: " + userId));
    }
}
