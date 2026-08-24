package com.fiap.techchallenge.inventory.schedules;

import com.fiap.techchallenge.inventory.entities.Part;
import com.fiap.techchallenge.inventory.entities.StockPolicy;
import com.fiap.techchallenge.inventory.repositories.StockPolicyRepository;
import com.fiap.techchallenge.inventory.services.StockPolicyEvaluator;
import com.fiap.techchallenge.shared.logging.LogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Safety net for {@link StockPolicyEvaluator}: the inline calls at reserve/adjust/receive/cancel
 * cover every event that can lower a part's position, but this catches anything that slips through —
 * a rule created against stale data, a missed call site in a future change — by re-checking every
 * rule once a night.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReorderSweep {

    private final StockPolicyRepository stockPolicyRepository;
    private final StockPolicyEvaluator evaluator;

    @Scheduled(cron = "${app.inventory.reorder-sweep-cron:0 0 4 * * ?}")
    @SchedulerLock(
            name = "ReorderSweep_sweep",
            lockAtLeastFor = "PT30S",
            lockAtMostFor = "PT10M"
    )
    protected void sweep() {
        try (LogContext.Scope ignored = LogContext.open(UUID.randomUUID().toString())) {
            int evaluated = runSweep();

            LogContext.put("job", "ReorderSweep");
            LogContext.put("evaluated", evaluated);
            log.info("scheduled_job");
        }
    }

    public int runSweep() {
        List<UUID> partIds = stockPolicyRepository.findAll().stream()
                .map(StockPolicy::getPart)
                .map(Part::getId)
                .toList();

        partIds.forEach(evaluator::evaluate);

        return partIds.size();
    }
}
