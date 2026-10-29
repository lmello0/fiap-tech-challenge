package com.fiap.workshop.workorder.adapter.in.web;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("workshop.webhooks")
record WebhookProperties(String budgetDecisionSecret, @DefaultValue("5m") Duration tolerance) {
}
