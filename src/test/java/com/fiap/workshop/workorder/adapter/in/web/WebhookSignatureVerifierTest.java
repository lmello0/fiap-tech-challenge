package com.fiap.workshop.workorder.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class WebhookSignatureVerifierTest {

	private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
	private static final String SECRET = "top-secret";
	private static final String BODY = "{\"eventId\":\"1\"}";

	private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(new WebhookProperties(SECRET, Duration.ofMinutes(5)),
			Clock.fixed(NOW, ZoneOffset.UTC));

	private static String timestamp(Instant instant) {
		return String.valueOf(instant.getEpochSecond());
	}

	private static String signature(String secret, String timestamp, String body) {
		return "sha256=" + WebhookSignatureVerifier.sign(secret, timestamp + "." + body);
	}

	@Test
	void acceptsAFreshCorrectlySignedRequest() {
		String ts = timestamp(NOW.minusSeconds(30));

		assertThat(verifier.isValid(ts, BODY, signature(SECRET, ts, BODY))).isTrue();
	}

	@Test
	void rejectsATamperedBodyOrTimestampOrSecret() {
		String ts = timestamp(NOW);
		String good = signature(SECRET, ts, BODY);

		assertThat(verifier.isValid(ts, BODY + " ", good)).isFalse();
		assertThat(verifier.isValid(timestamp(NOW.minusSeconds(1)), BODY, good)).isFalse();
		assertThat(verifier.isValid(ts, BODY, signature("another-secret", ts, BODY))).isFalse();
	}

	@Test
	void rejectsRequestsOutsideTheTimeWindowInEitherDirection() {
		String old = timestamp(NOW.minus(Duration.ofMinutes(6)));
		String future = timestamp(NOW.plus(Duration.ofMinutes(6)));
		String edge = timestamp(NOW.minus(Duration.ofMinutes(5)));

		assertThat(verifier.isValid(old, BODY, signature(SECRET, old, BODY))).isFalse();
		assertThat(verifier.isValid(future, BODY, signature(SECRET, future, BODY))).isFalse();
		assertThat(verifier.isValid(edge, BODY, signature(SECRET, edge, BODY))).isTrue();
	}

	@Test
	void rejectsMissingOrMalformedParts() {
		String ts = timestamp(NOW);
		String good = signature(SECRET, ts, BODY);

		assertThat(verifier.isValid(null, BODY, good)).isFalse();
		assertThat(verifier.isValid(ts, BODY, null)).isFalse();
		assertThat(verifier.isValid(ts, null, good)).isFalse();
		assertThat(verifier.isValid("yesterday", BODY, signature(SECRET, "yesterday", BODY))).isFalse();
		assertThat(verifier.isValid(ts, BODY, good.substring("sha256=".length()))).isFalse();
		assertThat(verifier.isValid(ts, BODY, "")).isFalse();
	}

	@Test
	void neverAcceptsAnythingWhenNoSecretIsConfigured() {
		var unconfigured = new WebhookSignatureVerifier(new WebhookProperties("", Duration.ofMinutes(5)), Clock.fixed(NOW, ZoneOffset.UTC));
		String ts = timestamp(NOW);

		assertThat(unconfigured.isValid(ts, BODY, signature(SECRET, ts, BODY))).isFalse();
		assertThat(new WebhookSignatureVerifier(new WebhookProperties(null, Duration.ofMinutes(5)), Clock.fixed(NOW, ZoneOffset.UTC))
				.isValid(ts, BODY, signature(SECRET, ts, BODY))).isFalse();
	}
}
