package com.fiap.techchallenge.shared.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurgeEventPublications {

    private static final String DELETE_ABANDONED = """
            DELETE FROM event_publication
             WHERE status = 'FAILED'
               AND publication_date < ?
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
        completedEventPublications.deletePublicationsOlderThan(properties.retention());

        Instant cutoff = Instant.now().minus(properties.retention());
        int abandoned = jdbcTemplate.update(DELETE_ABANDONED, Timestamp.from(cutoff));

        log.info("Purged completed event publications older than {} and {} abandoned publication(s)",
                properties.retention(), abandoned);
    }
}
