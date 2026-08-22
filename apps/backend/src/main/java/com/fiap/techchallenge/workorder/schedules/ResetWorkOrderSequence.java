package com.fiap.techchallenge.workorder.schedules;

import com.fiap.techchallenge.shared.logging.LogContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
public class ResetWorkOrderSequence {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?")
    @SchedulerLock(name = "ResetWorkOrderSequence_resetSequence")
    protected void resetSequence() {
        reset();
    }

    @Transactional
    public void reset() {
        try (LogContext.Scope ignored = LogContext.open(UUID.randomUUID().toString())) {
            entityManager
                    .createNativeQuery("SELECT setval('work_orders.seq_work_order_code', 1, false)")
                    .getSingleResult();

            LogContext.put("job", "ResetWorkOrderSequence");
            log.info("scheduled_job");
        }
    }
}
