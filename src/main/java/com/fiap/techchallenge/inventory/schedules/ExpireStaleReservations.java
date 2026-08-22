package com.fiap.techchallenge.inventory.schedules;

import com.fiap.techchallenge.inventory.api.PartReservationService;
import com.fiap.techchallenge.inventory.properties.InventoryProperties;
import com.fiap.techchallenge.shared.logging.LogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Releases reservations whose work order never started service. Inventory has no way to ask whether a
 * work order is still alive — one-way module dependency (see ADR 0006) — so age is the only signal
 * available: a reservation held past {@code app.inventory.reservation-ttl} is assumed abandoned.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpireStaleReservations {

    private final PartReservationService reservationService;
    private final InventoryProperties properties;

    @Scheduled(cron = "${app.inventory.reservation-expiry-cron:0 15 3 * * ?}")
    @SchedulerLock(
            name = "ExpireStaleReservations_expire",
            lockAtLeastFor = "PT30S",
            lockAtMostFor = "PT10M"
    )
    protected void expire() {
        runExpiry();
    }

    public int runExpiry() {
        try (LogContext.Scope ignored = LogContext.open(UUID.randomUUID().toString())) {
            Instant cutoff = Instant.now().minus(properties.reservationTtl());

            int expired = reservationService.expireStaleReservations(cutoff);

            LogContext.put("job", "ExpireStaleReservations");
            LogContext.put("expired", expired);
            LogContext.put("cutoff", cutoff);
            log.info("scheduled_job");

            return expired;
        }
    }
}
