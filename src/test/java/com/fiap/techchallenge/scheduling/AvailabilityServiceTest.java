package com.fiap.techchallenge.scheduling;

import com.fiap.techchallenge.scheduling.entities.Closure;
import com.fiap.techchallenge.scheduling.entities.SchedulingSettings;
import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import com.fiap.techchallenge.scheduling.exceptions.SlotUnavailableException;
import com.fiap.techchallenge.scheduling.properties.SchedulingProperties;
import com.fiap.techchallenge.scheduling.repositories.AppointmentRepository;
import com.fiap.techchallenge.scheduling.repositories.ClosureRepository;
import com.fiap.techchallenge.scheduling.repositories.SchedulingSettingsRepository;
import com.fiap.techchallenge.scheduling.services.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvailabilityServiceTest {

    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final ClosureRepository closureRepository = mock(ClosureRepository.class);
    private final SchedulingSettingsRepository settingsRepository = mock(SchedulingSettingsRepository.class);

    private final SchedulingProperties properties = new SchedulingProperties(
            Duration.ofHours(2), Duration.ofDays(30), Duration.ofDays(7), Duration.ofDays(7), Duration.ofDays(14));

    private final AvailabilityService service =
            new AvailabilityService(appointmentRepository, closureRepository, settingsRepository, properties);

    /** The next Monday, at 9:00 local time — always a workday, far enough out for every test. */
    private Instant nextMondayAt(int hour) {
        LocalDate date = LocalDate.now();
        while (date.getDayOfWeek() != DayOfWeek.MONDAY || !date.isAfter(LocalDate.now().plusDays(3))) {
            date = date.plusDays(1);
        }
        return ZonedDateTime.of(date, LocalTime.of(hour, 0), ZoneId.systemDefault()).toInstant();
    }

    @BeforeEach
    void setUp() {
        SchedulingSettings settings = new SchedulingSettings();
        settings.setId(UUID.randomUUID());
        settings.setBusinessStartTime(LocalTime.of(8, 0));
        settings.setBusinessEndTime(LocalTime.of(18, 0));
        settings.setDropoffSlotCapacity(3);
        settings.setPickupSlotCapacity(3);

        when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        when(closureRepository.existsByDate(any())).thenReturn(false);
        when(appointmentRepository.countByTypeAndSlotStartAndStatus(any(), any(), eq(AppointmentStatus.SCHEDULED)))
                .thenReturn(0L);
    }

    @Test
    void acceptsASlotWellInsideBusinessHours() {
        service.ensureAvailable(AppointmentType.DROPOFF, nextMondayAt(9));
    }

    @Test
    void rejectsASlotBeforeBusinessHours() {
        assertThatThrownBy(() -> service.ensureAvailable(AppointmentType.DROPOFF, nextMondayAt(7)))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("business hours");
    }

    @Test
    void rejectsASlotAtOrAfterBusinessEndTime() {
        assertThatThrownBy(() -> service.ensureAvailable(AppointmentType.DROPOFF, nextMondayAt(18)))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("business hours");
    }

    @Test
    void rejectsAWeekend() {
        LocalDate saturday = LocalDate.now();
        while (saturday.getDayOfWeek() != DayOfWeek.SATURDAY || !saturday.isAfter(LocalDate.now().plusDays(3))) {
            saturday = saturday.plusDays(1);
        }
        Instant slot = ZonedDateTime.of(saturday, LocalTime.of(9, 0), ZoneId.systemDefault()).toInstant();

        assertThatThrownBy(() -> service.ensureAvailable(AppointmentType.DROPOFF, slot))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("workday");
    }

    @Test
    void rejectsASlotInsideTheMinimumNoticeWindow() {
        Instant tooSoon = alignToSlot(Instant.now().plus(Duration.ofMinutes(30)));

        assertThatThrownBy(() -> service.ensureAvailable(AppointmentType.DROPOFF, tooSoon))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("minimum notice");
    }

    @Test
    void rejectsASlotBeyondTheMaximumLookahead() {
        Instant tooFar = alignToSlot(Instant.now().plus(Duration.ofDays(60)));

        assertThatThrownBy(() -> service.ensureAvailable(AppointmentType.DROPOFF, tooFar))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("maximum lookahead");
    }

    private static Instant alignToSlot(Instant instant) {
        long slotSeconds = Duration.ofMinutes(30).getSeconds();
        long epochSeconds = instant.getEpochSecond();
        return Instant.ofEpochSecond(epochSeconds - (epochSeconds % slotSeconds));
    }

    @Test
    void rejectsAClosedDate() {
        when(closureRepository.existsByDate(any())).thenReturn(true);

        assertThatThrownBy(() -> service.ensureAvailable(AppointmentType.DROPOFF, nextMondayAt(9)))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void rejectsAFullyBookedSlot() {
        when(appointmentRepository.countByTypeAndSlotStartAndStatus(any(), any(), eq(AppointmentStatus.SCHEDULED)))
                .thenReturn(3L);

        assertThatThrownBy(() -> service.ensureAvailable(AppointmentType.DROPOFF, nextMondayAt(9)))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("fully booked");
    }

    @Test
    void dropoffAndPickupCapacityAreCheckedIndependently() {
        when(appointmentRepository.countByTypeAndSlotStartAndStatus(
                eq(AppointmentType.DROPOFF), any(), eq(AppointmentStatus.SCHEDULED))).thenReturn(3L);
        when(appointmentRepository.countByTypeAndSlotStartAndStatus(
                eq(AppointmentType.PICKUP), any(), eq(AppointmentStatus.SCHEDULED))).thenReturn(0L);

        assertThatThrownBy(() -> service.ensureAvailable(AppointmentType.DROPOFF, nextMondayAt(9)))
                .isInstanceOf(SlotUnavailableException.class);

        service.ensureAvailable(AppointmentType.PICKUP, nextMondayAt(9));
    }

    @Test
    void availableSlotsReturnsNothingOnAClosedDate() {
        LocalDate monday = LocalDate.now();
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY || !monday.isAfter(LocalDate.now().plusDays(3))) {
            monday = monday.plusDays(1);
        }
        when(closureRepository.existsByDate(monday)).thenReturn(true);

        List<Instant> slots = service.availableSlots(AppointmentType.DROPOFF, monday);

        assertThat(slots).isEmpty();
    }
}
