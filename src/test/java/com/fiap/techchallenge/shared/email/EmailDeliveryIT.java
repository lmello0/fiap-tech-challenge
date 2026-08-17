package com.fiap.techchallenge.shared.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import com.fiap.techchallenge.email.schedules.RetryFailedEmails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Delivery, against the same Mailpit image {@code compose.yaml} runs.
 *
 * <p>The recovery test is the one that earns its keep: it proves the staleness settings actually
 * promote a stuck publication to FAILED. Misconfigure those and {@link RetryFailedEmails} degrades to
 * a silent no-op that no happy-path test would notice.
 */
@Testcontainers
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EmailDeliveryIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    @Container
    static final GenericContainer<?> MAILPIT = new GenericContainer<>(DockerImageName.parse("axllent/mailpit"))
            .withExposedPorts(1025, 8025)
            .withEnv("MP_SMTP_AUTH_ACCEPT_ANY", "1")
            .withEnv("MP_SMTP_AUTH_ALLOW_INSECURE", "1");

    @DynamicPropertySource
    static void mailpit(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", MAILPIT::getHost);
        registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    JavaMailSenderImpl mailSender;

    @Autowired
    RetryFailedEmails retryFailedEmails;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void plainTextEmailReachesEveryRecipient() throws Exception {
        String subject = uniqueSubject();

        publish(EmailRequestedEvent.builder()
                .to(List.of("first@example.com", "second@example.com"))
                .cc(List.of("copied@example.com"))
                .subject(subject)
                .plainText("Plain body")
                .build());

        JsonNode message = awaitMessage(subject);

        assertThat(recipients(message)).containsExactlyInAnyOrder("first@example.com", "second@example.com");
        assertThat(message.path("From").path("Address").asText()).isEqualTo("no-reply@techchallenge.test");
        assertThat(body(message).path("Text").asText()).contains("Plain body");
    }

    @Test
    void bothBodiesAreDeliveredAsMultipart() throws Exception {
        String subject = uniqueSubject();

        publish(EmailRequestedEvent.builder()
                .to(List.of("reader@example.com"))
                .subject(subject)
                .plainText("Text fallback")
                .html("<p>Rich body</p>")
                .build());

        JsonNode body = body(awaitMessage(subject));

        assertThat(body.path("Text").asText()).contains("Text fallback");
        assertThat(body.path("HTML").asText()).contains("<p>Rich body</p>");
    }

    @Test
    void anEmailThatFailedToSendGoesOutOnceTheMailServerIsBack() throws Exception {
        String subject = uniqueSubject();
        int workingPort = mailSender.getPort();

        mailSender.setPort(2);
        try {
            publish(EmailRequestedEvent.builder()
                    .to(List.of("patient@example.com"))
                    .subject(subject)
                    .plainText("Sent on the second attempt")
                    .expiresAt(Instant.now().plus(Duration.ofHours(1)))
                    .build());

            awaitFailedPublication(subject);
        } finally {
            mailSender.setPort(workingPort);
        }

        assertThat(findMessage(subject)).isEmpty();

        retryFailedEmails.resubmitFailedEmails();

        assertThat(body(awaitMessage(subject)).path("Text").asText()).contains("Sent on the second attempt");
    }

    @Test
    void anExpiredEmailIsNeverSentAndIsNotRecordedAsCompleted() throws Exception {
        String subject = uniqueSubject();

        publish(EmailRequestedEvent.builder()
                .to(List.of("toolate@example.com"))
                .subject(subject)
                .plainText("Should never arrive")
                .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
                .build());

        awaitFailedPublication(subject);

        assertThat(findMessage(subject)).isEmpty();

        // Retrying must not resurrect it either — the filter drops anything past its expiry.
        retryFailedEmails.resubmitFailedEmails();
        Thread.sleep(1_000);

        assertThat(findMessage(subject)).isEmpty();
        assertThat(completedPublications(subject)).isZero();
    }

    private void publish(EmailRequestedEvent event) {
        // @ApplicationModuleListener dispatches after commit, so there has to be a commit.
        transactions.executeWithoutResult(status -> publisher.publishEvent(event));
    }

    private JsonNode awaitMessage(String subject) throws Exception {
        return await(() -> findMessage(subject), "no email with subject " + subject + " arrived");
    }

    private void awaitFailedPublication(String subject) throws Exception {
        await(() -> {
            Integer failed = jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM event_publication
                     WHERE status = 'FAILED' AND serialized_event LIKE ?
                    """, Integer.class, "%" + subject + "%");

            return Optional.ofNullable(failed).filter(count -> count > 0);
        }, "publication for " + subject + " was never marked FAILED");
    }

    private int completedPublications(String subject) {
        Integer completed = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM event_publication
                 WHERE completion_date IS NOT NULL AND serialized_event LIKE ?
                """, Integer.class, "%" + subject + "%");

        return completed == null ? 0 : completed;
    }

    private <T> T await(ThrowingSupplier<Optional<T>> condition, String failureMessage) throws Exception {
        Instant deadline = Instant.now().plus(TIMEOUT);

        while (Instant.now().isBefore(deadline)) {
            Optional<T> result = condition.get();

            if (result.isPresent()) {
                return result.get();
            }

            Thread.sleep(250);
        }

        throw new AssertionError(failureMessage);
    }

    private Optional<JsonNode> findMessage(String subject) throws IOException, InterruptedException {
        JsonNode messages = get("/api/v1/messages?limit=200").path("messages");

        for (JsonNode message : messages) {
            if (subject.equals(message.path("Subject").asText())) {
                return Optional.of(message);
            }
        }

        return Optional.empty();
    }

    private JsonNode body(JsonNode message) throws IOException, InterruptedException {
        return get("/api/v1/message/" + message.path("ID").asText());
    }

    private static List<String> recipients(JsonNode message) {
        return message.path("To").valueStream()
                .map(recipient -> recipient.path("Address").asText())
                .toList();
    }

    private JsonNode get(String path) throws IOException, InterruptedException {
        URI uri = URI.create("http://%s:%d%s".formatted(MAILPIT.getHost(), MAILPIT.getMappedPort(8025), path));

        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());

        return json.readTree(response.body());
    }

    private static String uniqueSubject() {
        return "subject-" + UUID.randomUUID();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
