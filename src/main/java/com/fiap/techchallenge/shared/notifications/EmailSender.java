package com.fiap.techchallenge.shared.notifications;

public interface EmailSender {

    void sendPasswordReset(String toEmail, String rawToken);

    void sendEmailVerification(String toEmail, String rawToken);

    void sendEmailChange(String toEmail, String rawToken);
}
