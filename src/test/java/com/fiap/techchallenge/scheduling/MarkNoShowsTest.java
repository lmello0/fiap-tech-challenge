package com.fiap.techchallenge.scheduling;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.scheduling.entities.Appointment;
import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import com.fiap.techchallenge.scheduling.repositories.AppointmentRepository;
import com.fiap.techchallenge.scheduling.schedules.MarkNoShows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.fiap.techchallenge.scheduling.entities.Appointment.SLOT_DURATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * CONTEXT.md "No-Show": a SCHEDULED Appointment whose slot end time has passed with no Check-in.
 * Same shape as {@code inventory.schedules.ExpireStaleReservationsTest} would be for its module.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@RecordApplicationEvents
class MarkNoShowsTest {

    @Autowired
    MarkNoShows job;

    @Autowired
    AppointmentRepository repository;

    @Autowired
    ApplicationEvents events;

    @Test
    void marksOnlyScheduledAppointmentsPastTheirSlotEndAndPublishesAnEventForEach() {
        UUID overdueId = persist(AppointmentStatus.SCHEDULED, Instant.now().minus(SLOT_DURATION).minus(Duration.ofMinutes(1)));
        UUID stillWithinSlot = persist(AppointmentStatus.SCHEDULED, Instant.now().minus(Duration.ofMinutes(5)));
        UUID alreadyCompleted = persist(AppointmentStatus.COMPLETED, Instant.now().minus(Duration.ofHours(2)));

        int marked = job.markOverdueAppointmentsNoShow();

        assertThat(marked).isEqualTo(1);
        assertThat(repository.findById(overdueId).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(repository.findById(stillWithinSlot).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(repository.findById(alreadyCompleted).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.COMPLETED);

        assertThat(events.stream(com.fiap.techchallenge.scheduling.api.events.AppointmentNoShowEvent.class)
                .anyMatch(event -> event.appointmentId().equals(overdueId))).isTrue();
    }

    private UUID persist(AppointmentStatus status, Instant slotStart) {
        Appointment appointment = new Appointment();
        appointment.setType(AppointmentType.DROPOFF);
        appointment.setStatus(status);
        appointment.setSlotStart(slotStart);
        appointment.setGuestName("No-Show Guest");
        appointment.setGuestPhone(uniquePhone());
        appointment.setGuestEmail(uniqueEmail());
        appointment.setGuestVehicleMake("Toyota");
        appointment.setGuestVehicleModel("Corolla");
        appointment.setGuestVehicleYear(2020);
        appointment.setComplaint("x");

        return repository.save(appointment).getId();
    }

    private static String uniquePhone() {
        return "119" + ThreadLocalRandom.current().nextInt(10000000, 99999999);
    }

    private static String uniqueEmail() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com";
    }
}
