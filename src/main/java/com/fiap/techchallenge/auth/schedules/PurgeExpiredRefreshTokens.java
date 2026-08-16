package com.fiap.techchallenge.auth.schedules;

import com.fiap.techchallenge.auth.properties.JwtProperties;
import com.fiap.techchallenge.auth.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;


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
        Instant cutoff = Instant.now().minus(RETENTION_AFTER_EXPIRY);

        int deleted = refreshTokenService.purgeExpiredBefore(cutoff);

        log.info("Purged {} refresh token(s) expired before {} (retention={} past a {} TTL)",
                deleted, cutoff, RETENTION_AFTER_EXPIRY, properties.refreshTokenTTL());
    }
}
