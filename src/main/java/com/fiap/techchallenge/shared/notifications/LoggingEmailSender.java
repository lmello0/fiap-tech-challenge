package com.fiap.techchallenge.shared.notifications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stand-in for a real email provider. Logs the token instead of delivering it, so the
 * verification/reset flows can be built and tested before an SMTP/SES provider is wired in.
 */
@Slf4j
@Component
public class LoggingEmailSender implements EmailSender {

    @Override
    public void sendPasswordReset(String toEmail, String rawToken) {
        log.info("[email:stub] Password reset requested for {} — token={}", toEmail, rawToken);
    }

    @Override
    public void sendEmailVerification(String toEmail, String rawToken) {
        log.info("[email:stub] Email verification requested for {} — token={}", toEmail, rawToken);
    }

    @Override
    public void sendEmailChange(String toEmail, String rawToken) {
        log.info("[email:stub] Email change confirmation requested for {} — token={}", toEmail, rawToken);
    }
}
