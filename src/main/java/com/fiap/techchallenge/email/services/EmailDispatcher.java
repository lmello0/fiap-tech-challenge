package com.fiap.techchallenge.email.services;

import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import com.fiap.techchallenge.email.exceptions.EmailExpiredException;
import com.fiap.techchallenge.email.properties.EmailProperties;
import com.fiap.techchallenge.email.schedules.RetryFailedEmails;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * The only place in the application that actually sends mail. Package-private and outside
 * {@code shared.email.api}, so no other module can reach it — reaching the mailer is done by
 * publishing an {@link EmailRequestedEvent} and nothing else (see ADR 0004).
 *
 * <p>{@code @ApplicationModuleListener} dispatches after the publishing transaction commits, on an
 * async thread, with the publication persisted in {@code event_publication}. A send that throws
 * leaves that row incomplete, and {@link RetryFailedEmails} picks it up on a later tick.
 *
 * <p>Recipient addresses are deliberately kept out of the logs, matching the auth services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDispatcher {

    private final JavaMailSender mailSender;
    private final EmailProperties properties;

    @ApplicationModuleListener
    void on(EmailRequestedEvent event) throws Exception {
        if (event.isExpired(Instant.now())) {
            log.warn("Dropping expired email subject='{}' expiresAt={} recipients={}",
                    event.subject(), event.expiresAt(), event.to().size());

            throw new EmailExpiredException("Email expired before delivery: " + event.subject());
        }

        if (event.html() == null || event.html().isBlank()) {
            mailSender.send(plainMessage(event));
        } else {
            mailSender.send(mimeMessage(event));
        }

        log.info("Email sent subject='{}' recipients={} multipart={}",
                event.subject(), event.to().size(), event.isMultipart());
    }

    private SimpleMailMessage plainMessage(EmailRequestedEvent event) throws UnsupportedEncodingException {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(sender());
        message.setTo(toArray(event.to()));
        message.setSubject(event.subject());
        message.setText(event.plainText());

        if (!event.cc().isEmpty()) {
            message.setCc(toArray(event.cc()));
        }
        if (!event.bcc().isEmpty()) {
            message.setBcc(toArray(event.bcc()));
        }

        return message;
    }

    private MimeMessage mimeMessage(EmailRequestedEvent event) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, event.isMultipart(), StandardCharsets.UTF_8.name());

        helper.setFrom(sender());
        helper.setTo(toArray(event.to()));
        helper.setSubject(event.subject());

        if (event.isMultipart()) {
            helper.setText(event.plainText(), event.html());
        } else {
            helper.setText(event.html(), true);
        }

        if (!event.cc().isEmpty()) {
            helper.setCc(toArray(event.cc()));
        }
        if (!event.bcc().isEmpty()) {
            helper.setBcc(toArray(event.bcc()));
        }

        return message;
    }

    private String sender() throws UnsupportedEncodingException {
        return new InternetAddress(properties.from(), properties.fromName(), StandardCharsets.UTF_8.name())
                .toString();
    }

    private static String[] toArray(List<String> recipients) {
        return recipients.toArray(String[]::new);
    }
}
