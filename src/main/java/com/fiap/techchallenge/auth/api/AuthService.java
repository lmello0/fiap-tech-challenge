package com.fiap.techchallenge.auth.api;

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
import com.fiap.techchallenge.user.api.representation.UserInfo;

import java.util.UUID;

public interface AuthService {

    /**
     * Self-service registration: creates the user with the customer facet and its local credential,
     * then signs them straight in.
     */
    TokenResponse registerCustomer(RegisterCustomerCommand command);

    /**
     * Staff registration, performed by a manager. Issues no tokens — the worker signs in themselves.
     */
    UserInfo registerWorker(RegisterWorkerCommand command);

    TokenResponse login(LoginCommand command);

    TokenResponse refresh(String rawRefreshToken);

    /**
     * Ends the single session the given refresh token belongs to.
     */
    void logout(String rawRefreshToken);

    /**
     * Ends every session of the user. Access tokens already issued remain valid until they expire.
     */
    void logoutEverywhere(UUID userId);

    /**
     * Changes the local password, verifying the current one first, and revokes every refresh token
     * so old sessions cannot outlive it.
     */
    void changePassword(UUID userId, ChangePasswordCommand command);

    /**
     * Always succeeds from the caller's point of view, whether or not the email belongs to an
     * account — this is what keeps the endpoint from leaking which emails are registered.
     */
    void requestPasswordReset(RequestPasswordResetCommand command);

    /**
     * Consumes the magic-link token and sets the new password. Revokes every refresh token for
     * the account, so a compromised session cannot outlive the reset.
     */
    void confirmPasswordReset(ConfirmPasswordResetCommand command);

    void confirmEmailVerification(ConfirmEmailVerificationCommand command);

    void resendEmailVerification(ResendEmailVerificationCommand command);

    /**
     * Sends a confirmation link to the new address. The registered email does not change until
     * that link is followed (double opt-in).
     */
    void requestEmailChange(UUID userId, RequestEmailChangeCommand command);

    void confirmEmailChange(ConfirmEmailChangeCommand command);
}
