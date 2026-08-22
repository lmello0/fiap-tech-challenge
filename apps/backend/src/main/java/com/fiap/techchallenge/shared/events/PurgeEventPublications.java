package com.fiap.techchallenge.shared.events;

import com.fiap.techchallenge.shared.logging.LogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Keeps the {@code event_publication} table from growing without bound.
 *
 * <p>Registry-wide on purpose: it collects every module's publications, not just the email module's,
 * which is why it lives here rather than in {@code shared.email}. Retention matches the refresh token
 * purge at 30 days.
 *
 * <p>Two passes, because Modulith's public API only covers one of them. Completed publications go
 * through {@link CompletedEventPublications}. Publications abandoned as FAILED — an email that
 * exhausted its attempts or expired undelivered — are only reachable through
 * {@code EventPublicationRepository}, which ships runtime-scoped and so cannot be compiled against;
 * they are deleted with SQL instead, against the same schema this project already owns in
 * {@code V1__CREATE_SPRING_EVENTS_TABLE.sql}.
 *
 * <p>{@code history}'s own listener is excluded from that delete (ADR 0011): {@code history} never
 * deletes what it has already written, but a FAILED publication is a row it never got to write in
 * the first place — deleting the publication here would erase the only trace that an event was ever
 * supposed to become a history entry, with no compensating record anywhere.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurgeEventPublications {

    private static final String DELETE_ABANDONED = """
            DELETE FROM event_publication
             WHERE status = 'FAILED'
               AND publication_date < ?
               AND listener_id NOT LIKE 'com.fiap.techchallenge.history.%'
            """;

    private final CompletedEventPublications completedEventPublications;
    private final JdbcTemplate jdbcTemplate;
    private final EventPublicationProperties properties;

    @Scheduled(cron = "${app.events.purge-cron:0 45 3 * * ?}")
    @SchedulerLock(
            name = "PurgeEventPublications_purge",
            lockAtLeastFor = "PT30S",
            lockAtMostFor = "PT10M"
    )
    protected void purge() {
        try (LogContext.Scope ignored = LogContext.open(UUID.randomUUID().toString())) {
            completedEventPublications.deletePublicationsOlderThan(properties.retention());

            Instant cutoff = Instant.now().minus(properties.retention());
            int abandoned = jdbcTemplate.update(DELETE_ABANDONED, Timestamp.from(cutoff));

            LogContext.put("job", "PurgeEventPublications");
            LogContext.put("abandoned", abandoned);
            LogContext.put("retention", properties.retention());
            log.info("scheduled_job");
        }
    }
}
