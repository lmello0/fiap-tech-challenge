package com.fiap.techchallenge.scheduling.schedules;

import com.fiap.techchallenge.scheduling.api.events.AppointmentNoShowEvent;
import com.fiap.techchallenge.scheduling.api.representation.AppointmentSnapshot;
import com.fiap.techchallenge.scheduling.entities.Appointment;
import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;
import com.fiap.techchallenge.scheduling.mappers.AppointmentMapper;
import com.fiap.techchallenge.scheduling.repositories.AppointmentRepository;
import com.fiap.techchallenge.shared.audit.ActorResolver;
import com.fiap.techchallenge.shared.logging.LogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Marks a SCHEDULED Appointment NO_SHOW once its slot end time has passed with no Check-in
 * (CONTEXT.md "No-Show") — same shape as {@code inventory.schedules.ExpireStaleReservations}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarkNoShows {

    private final AppointmentRepository repository;
    private final AppointmentMapper mapper;
    private final ApplicationEventPublisher events;
    private final ActorResolver actorResolver;

    @Scheduled(cron = "${app.scheduling.no-show-sweep-cron:0 */15 * * * ?}")
    @SchedulerLock(
            name = "MarkNoShows_sweep",
            lockAtLeastFor = "PT30S",
            lockAtMostFor = "PT5M"
    )
    protected void sweep() {
        markOverdueAppointmentsNoShow();
    }

    /**
     * Split from {@link #sweep()} so it can be driven without ShedLock's lock (see
     * {@code email.schedules.RetryFailedEmails} for the original precedent).
     */
    @Transactional
    public int markOverdueAppointmentsNoShow() {
        try (LogContext.Scope ignored = LogContext.open(UUID.randomUUID().toString())) {
            Instant cutoff = Instant.now().minus(Appointment.SLOT_DURATION);

            List<Appointment> overdue = repository.findByStatusAndSlotStartLessThan(AppointmentStatus.SCHEDULED, cutoff);

            for (Appointment appointment : overdue) {
                appointment.markNoShow();
                repository.save(appointment);

                events.publishEvent(new AppointmentNoShowEvent(
                        appointment.getId(),
                        actorResolver.forSystem("MarkNoShows", true),
                        new AppointmentSnapshot(mapper.toInfo(appointment))
                ));
            }

            LogContext.put("job", "MarkNoShows");
            LogContext.put("marked", overdue.size());
            LogContext.put("cutoff", cutoff);
            log.info("scheduled_job");

            return overdue.size();
        }
    }
}
