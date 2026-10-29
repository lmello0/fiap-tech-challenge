package com.fiap.workshop.notification.adapter.out.mail;

import com.fiap.workshop.notification.application.port.out.AdminContact;
import com.fiap.workshop.notification.application.port.out.MailSender;
import jakarta.mail.MessagingException;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@EnableConfigurationProperties(NotificationProperties.class)
class SmtpMailSender implements MailSender, AdminContact {

	private final JavaMailSender javaMail;
	private final TemplateEngine templates;
	private final NotificationProperties properties;

	SmtpMailSender(JavaMailSender javaMail, TemplateEngine templates, NotificationProperties properties) {
		this.javaMail = javaMail;
		this.templates = templates;
		this.properties = properties;
	}

	@Override
	public void send(String to, String subject, String template, Map<String, Object> variables) {
		Context context = new Context();
		context.setVariables(variables);
		String html = templates.process("mail/" + template, context);
		try {
			var message = javaMail.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
			helper.setFrom(properties.from());
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(html, true);
			javaMail.send(message);
		} catch (MessagingException e) {
			throw new IllegalStateException("Could not build the e-mail to " + to, e);
		}
	}

	@Override
	public String email() {
		return properties.adminEmail();
	}
}
