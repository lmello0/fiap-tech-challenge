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
import com.fiap.techchallenge.auth.api.representation.TemporaryCredential;
import com.fiap.techchallenge.auth.api.representation.TokenResponse;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;

import java.util.Base64;
import com.fiap.techchallenge.auth.entities.UserAuth;
import com.fiap.techchallenge.auth.enums.AuthProvider;
import com.fiap.techchallenge.auth.exceptions.AccountDisabledException;
import com.fiap.techchallenge.auth.exceptions.AccountLockedException;
import com.fiap.techchallenge.auth.exceptions.EmailNotVerifiedException;
import com.fiap.techchallenge.auth.exceptions.InvalidTokenException;
import com.fiap.techchallenge.auth.exceptions.RegistrationConflictException;
import com.fiap.techchallenge.auth.properties.LoginRateLimitProperties;
import com.fiap.techchallenge.auth.repositories.UserAuthRepository;
import com.fiap.techchallenge.shared.logging.LogContext;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.user.api.representation.UserPrincipal;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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

    /**
     * Base64's alphabet has exactly 64 symbols, so each character of a random base64 string can
     * index straight into a 64-word list — no separate random-number step needed, and the result
     * reads aloud far more easily than a raw random string (e.g. "River-Comet-Otter-Willow-42").
     * This never has to survive past the customer's first login, where {@code needPasswordChange}
     * forces a real one, so a handful of words plus a two-digit suffix is plenty.
     */
    private static final String BASE64_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private static final String[] PASSPHRASE_WORDS = {
            "Apple", "River", "Comet", "Puzzle", "Otter", "Maple", "Cedar", "Falcon",
            "Garden", "Harbor", "Island", "Jungle", "Kettle", "Lantern", "Meadow", "Nectar",
            "Ocean", "Pebble", "Quartz", "Rocket", "Summit", "Timber", "Velvet", "Willow",
            "Yonder", "Zephyr", "Amber", "Breeze", "Canyon", "Delta", "Ember", "Frost",
            "Glacier", "Horizon", "Indigo", "Jasper", "Kite", "Lagoon", "Marble", "Nimbus",
            "Orbit", "Prairie", "Quiver", "Ripple", "Sable", "Thunder", "Umber", "Violet",
            "Walnut", "Xenon", "Yarrow", "Zenith", "Anchor", "Basil", "Coral", "Dune",
            "Echo", "Fable", "Grove", "Haven", "Ivory", "Juniper", "Knoll", "Lumen"
    };

    private static final int PASSPHRASE_WORD_COUNT = 2;

    private final StringKeyGenerator temporaryPassphraseGenerator =
            new Base64StringKeyGenerator(Base64.getEncoder().withoutPadding(), 24);

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

            LogContext.put("userId", user.id());

            return issueFor(principalOf(user.id()), false);
        } catch (DataIntegrityViolationException e) {
            LogContext.put("outcome", "registration_conflict");
            throw new RegistrationConflictException();
        }
    }

    @Override
    @Transactional
    public UserInfo registerWorker(RegisterWorkerCommand command) {
        try {
            UserInfo user = userService.createWorker(command.worker());

            // Someone else picked this password — the manager onboarding them, or the bootstrap
            // runner reading it out of the environment — so the worker must rotate it before use.
            userAuthRepository.save(UserAuth.localWithTemporaryPassword(
                    user.id(), passwordEncoder.encode(command.rawPassword())));
            emailVerificationService.issueEmailVerification(user.id(), user.email());

            LogContext.put("userId", user.id());

            return user;
        } catch (DataIntegrityViolationException e) {
            LogContext.put("outcome", "registration_conflict");
            throw new RegistrationConflictException();
        }
    }

    @Override
    @Transactional
    public TemporaryCredential registerCustomerWithTemporaryPassword(CreateUserCommand command) {
        try {
            UserInfo user = userService.createCustomer(command);

            String rawPassword = generateTemporaryPassphrase();
            userAuthRepository.save(UserAuth.localWithTemporaryPassword(
                    user.id(), passwordEncoder.encode(rawPassword)));
            emailVerificationService.issueEmailVerification(user.id(), user.email());

            LogContext.put("userId", user.id());

            return new TemporaryCredential(user, rawPassword);
        } catch (DataIntegrityViolationException e) {
            LogContext.put("outcome", "registration_conflict");
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
            LogContext.put("userId", user.id());
            LogContext.put("outcome", "account_locked");
            throw new AccountLockedException("Account is temporarily locked due to too many failed attempts");
        }

        String hashToCheck = credential
                .map(UserAuth::getPasswordHash)
                .orElse(dummyHash);

        boolean passwordOk = passwordEncoder.matches(command.rawPassword(), hashToCheck);

        if (user == null || !passwordOk) {
            credential.ifPresent(c -> c.registerFailedAttempt(now, rateLimitProperties.maxAttempts(), rateLimitProperties.lockDuration()));
            LogContext.put("outcome", "invalid_credentials");
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.emailVerified()) {
            LogContext.put("userId", user.id());
            LogContext.put("outcome", "email_not_verified");
            throw new EmailNotVerifiedException("Email address has not been verified yet");
        }

        ensureActive(user);
        credential.ifPresent(UserAuth::resetFailedAttempts);

        LogContext.put("userId", user.id());
        return issueFor(user, credential.map(UserAuth::isNeedPasswordChange).orElse(false));
    }

    @Override
    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);

        UserPrincipal user = userService.findPrincipalById(rotation.userId())
                .orElseThrow(() -> new InvalidTokenException("Unknown token subject"));

        ensureActive(user);

        // Re-read the flag rather than trusting the expiring token that asked for this one: without
        // it, refreshing would be a way to trade a flagged token for a clean one.
        boolean needPasswordChange = userAuthRepository
                .findByUserIdAndProvider(user.id(), AuthProvider.LOCAL)
                .map(UserAuth::isNeedPasswordChange)
                .orElse(false);

        return new TokenResponse(
                jwtService.issueAccessToken(user, authorities(user), needPasswordChange),
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
        LogContext.put("userId", userId);
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
            LogContext.put("userId", userId);
            LogContext.put("outcome", "invalid_current_password");
            throw new BadCredentialsException("Invalid credentials");
        }

        credential.changePassword(passwordEncoder.encode(command.newPassword()));
        refreshTokenService.revokeAllForUser(userId);

        LogContext.put("userId", userId);
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

    private TokenResponse issueFor(UserPrincipal user, boolean needPasswordChange) {
        return new TokenResponse(
                jwtService.issueAccessToken(user, authorities(user), needPasswordChange),
                refreshTokenService.issue(user.id()),
                jwtService.accessTokenTTL()
        );
    }

    private void ensureActive(UserPrincipal user) {
        if (!user.enabled()) {
            LogContext.put("userId", user.id());
            LogContext.put("outcome", "account_disabled");
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

    private String generateTemporaryPassphrase() {
        String raw = temporaryPassphraseGenerator.generateKey();

        StringBuilder passphrase = new StringBuilder();
        for (int i = 0; i < PASSPHRASE_WORD_COUNT; i++) {
            int wordIndex = BASE64_ALPHABET.indexOf(raw.charAt(i));
            passphrase.append(PASSPHRASE_WORDS[wordIndex]).append('-');
        }

        int suffix = (BASE64_ALPHABET.indexOf(raw.charAt(PASSPHRASE_WORD_COUNT)) * 64
                + BASE64_ALPHABET.indexOf(raw.charAt(PASSPHRASE_WORD_COUNT + 1))) % 100;
        passphrase.append(suffix);

        return passphrase.toString();
    }
}
