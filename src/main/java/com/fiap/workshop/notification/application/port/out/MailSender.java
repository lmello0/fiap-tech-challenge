package com.fiap.workshop.notification.application.port.out;

import java.util.Map;

public interface MailSender {

	void send(String to, String subject, String template, Map<String, Object> variables);
}
