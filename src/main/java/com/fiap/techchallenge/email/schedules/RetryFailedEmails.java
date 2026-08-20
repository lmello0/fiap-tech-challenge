package com.fiap.techchallenge.email.schedules;

import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import com.fiap.techchallenge.email.properties.EmailProperties;
import com.fiap.techchallenge.shared.logging.LogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Resends emails whose delivery failed. Spring Modulith persists every publication but never
 * resubmits one on its own — that trigger is this job (see ADR 0004).
 *
 * <p>Only publications the staleness monitor has marked FAILED are eligible, so a send still in
 * flight is never resubmitted underneath itself. Rows come back oldest-first, so a backlog drains in
 * the order it built up.
 *
 * <p>The filter is what keeps this job honest: it only touches {@link EmailRequestedEvent}
 * publications (other modules' events are none of its business), skips messages past their
 * {@code expiresAt}, and gives up after {@code app.mail.max-attempts} passes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryFailedEmails {

    private final FailedEventPublications failedEventPublications;
    private final EmailProperties properties;

    @Scheduled(fixedDelayString = "${app.mail.retry-delay:1m}")
    @SchedulerLock(
            name = "RetryFailedEmails_retry",
            lockAtLeastFor = "PT10S",
            lockAtMostFor = "PT2M"
    )
    protected void retry() {
        resubmitFailedEmails();
    }

    /**
     * The work itself, split from {@link #retry()} so it can be driven without ShedLock's lock: with
     * the default PROXY_METHOD interception a direct call to the scheduled method is skipped whenever
     * the lock is held.
     */
    public void resubmitFailedEmails() {
        try (LogContext.Scope ignored = LogContext.open(UUID.randomUUID().toString())) {
            Instant now = Instant.now();
            Counts counts = new Counts();

            failedEventPublications.resubmit(ResubmissionOptions.defaults()
                    .withFilter(publication -> isRetryableEmail(publication, now, counts)));

            LogContext.put("job", "RetryFailedEmails");
            LogContext.put("resubmitted", counts.resubmitted);
            LogContext.put("expired", counts.expired);
            LogContext.put("attemptsExhausted", counts.attemptsExhausted);
            log.info("scheduled_job");
        }
    }

    private boolean isRetryableEmail(EventPublication publication, Instant now, Counts counts) {
        if (!(publication.getEvent() instanceof EmailRequestedEvent email)) {
            return false;
        }

        if (email.isExpired(now)) {
            counts.expired++;
            // Terminal: unlike a resubmission, nothing ever gets its own canonical line for this
            // email again, so its abandonment is worth one here.
            log.warn("email_retry_abandoned subject='{}' reason=expired expiresAt={}", email.subject(), email.expiresAt());
            return false;
        }

        if (publication.getCompletionAttempts() >= properties.maxAttempts()) {
            counts.attemptsExhausted++;
            log.warn("email_retry_abandoned subject='{}' reason=attempts_exhausted attempts={}",
                    email.subject(), publication.getCompletionAttempts());
            return false;
        }

        counts.resubmitted++;
        return true;
    }

    /** Per-run tallies for the one canonical line {@link #resubmitFailedEmails()} logs (ADR 0017). */
    private static final class Counts {
        int resubmitted;
        int expired;
        int attemptsExhausted;
    }
}
