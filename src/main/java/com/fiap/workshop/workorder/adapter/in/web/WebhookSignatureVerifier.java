package com.fiap.workshop.workorder.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
class WebhookSignatureVerifier {

	private static final String PREFIX = "sha256=";
	private static final String ALGORITHM = "HmacSHA256";

	private final WebhookProperties properties;
	private final Clock clock;

	WebhookSignatureVerifier(WebhookProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	boolean isValid(String timestamp, String body, String signature) {
		String secret = properties.budgetDecisionSecret();
		if (secret == null || secret.isBlank() || timestamp == null || signature == null || body == null) {
			return false;
		}
		if (!isFresh(timestamp)) {
			return false;
		}
		String expected = PREFIX + sign(secret, timestamp + "." + body);
		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.trim().getBytes(StandardCharsets.UTF_8));
	}

	static String sign(String secret, String content) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
			return HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("HMAC-SHA256 is not available", e);
		}
	}

	private boolean isFresh(String timestamp) {
		try {
			Instant sent = Instant.ofEpochSecond(Long.parseLong(timestamp.trim()));
			Duration age = Duration.between(sent, clock.instant()).abs();
			return age.compareTo(properties.tolerance()) <= 0;
		} catch (NumberFormatException | java.time.DateTimeException e) {
			return false;
		}
	}
}
