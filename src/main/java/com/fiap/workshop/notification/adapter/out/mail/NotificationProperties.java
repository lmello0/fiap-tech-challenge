package com.fiap.workshop.notification.adapter.out.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("workshop.notification")
record NotificationProperties(String from, String adminEmail) {
}
