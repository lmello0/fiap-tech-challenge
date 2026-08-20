package com.fiap.techchallenge.auth.schedules;

import com.fiap.techchallenge.auth.properties.JwtProperties;
import com.fiap.techchallenge.auth.services.RefreshTokenService;
import com.fiap.techchallenge.shared.logging.LogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class PurgeExpiredRefreshTokens {

    private static final Duration RETENTION_AFTER_EXPIRY = Duration.ofDays(30);

    private final RefreshTokenService refreshTokenService;
    private final JwtProperties properties;

    @Scheduled(cron = "${app.auth.purge-expired-tokens-cron:0 30 3 * * ?}")
    @SchedulerLock(
            name = "PurgeExpiredRefreshTokens_purge",
            lockAtLeastFor = "PT30S",
            lockAtMostFor = "PT10M"
    )
    protected void purge() {
        try (LogContext.Scope ignored = LogContext.open(UUID.randomUUID().toString())) {
            Instant cutoff = Instant.now().minus(RETENTION_AFTER_EXPIRY);

            int deleted = refreshTokenService.purgeExpiredBefore(cutoff);

            LogContext.put("job", "PurgeExpiredRefreshTokens");
            LogContext.put("deleted", deleted);
            LogContext.put("cutoff", cutoff);
            LogContext.put("retention", RETENTION_AFTER_EXPIRY);
            log.info("scheduled_job");
        }
    }
}
