package com.fiap.techchallenge.scheduling.services;

import com.fiap.techchallenge.scheduling.entities.Appointment;
import com.fiap.techchallenge.scheduling.entities.SchedulingSettings;
import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import com.fiap.techchallenge.scheduling.exceptions.SlotUnavailableException;
import com.fiap.techchallenge.scheduling.properties.SchedulingProperties;
import com.fiap.techchallenge.scheduling.repositories.AppointmentRepository;
import com.fiap.techchallenge.scheduling.repositories.ClosureRepository;
import com.fiap.techchallenge.scheduling.repositories.SchedulingSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Validates a candidate slot against the Operating Calendar (CONTEXT.md): fixed Monday-Friday
 * workdays, Manager-configured business hours and per-type Slot Capacity, Manager Closures, and the
 * booking window (minimum notice / maximum lookahead). Enforced only in the application, inside the
 * booking transaction — same rigor as "exactly one live Budget" (ADR 0013), no DB-level constraint.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    /** Workdays are a fixed constant, not Manager-configurable, per the confirmed design. */
    private static final Set<DayOfWeek> WORKDAYS = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private static final ZoneId SHOP_ZONE = ZoneId.systemDefault();

    private final AppointmentRepository appointmentRepository;
    private final ClosureRepository closureRepository;
    private final SchedulingSettingsRepository settingsRepository;
    private final SchedulingProperties properties;

    public void ensureAvailable(AppointmentType type, Instant slotStart) {
        Instant now = Instant.now();
        SchedulingSettings settings = currentSettings();

        if (!slotStart.equals(alignToSlot(slotStart))) {
            throw new SlotUnavailableException(type, slotStart, "not aligned to a 30-minute slot boundary");
        }

        if (slotStart.isBefore(now.plus(properties.minNotice()))) {
            throw new SlotUnavailableException(type, slotStart, "inside the minimum notice window");
        }

        if (slotStart.isAfter(now.plus(properties.maxLookahead()))) {
            throw new SlotUnavailableException(type, slotStart, "beyond the maximum lookahead window");
        }

        ZonedDateTime local = slotStart.atZone(SHOP_ZONE);
        LocalDate date = local.toLocalDate();
        LocalTime time = local.toLocalTime();

        if (!WORKDAYS.contains(local.getDayOfWeek())) {
            throw new SlotUnavailableException(type, slotStart, "not a workday");
        }

        if (time.isBefore(settings.getBusinessStartTime()) || !time.isBefore(settings.getBusinessEndTime())) {
            throw new SlotUnavailableException(type, slotStart, "outside business hours");
        }

        if (closureRepository.existsByDate(date)) {
            throw new SlotUnavailableException(type, slotStart, "date is closed");
        }

        long booked = appointmentRepository.countByTypeAndSlotStartAndStatus(type, slotStart, AppointmentStatus.SCHEDULED);
        if (booked >= settings.capacityFor(type)) {
            throw new SlotUnavailableException(type, slotStart, "slot is fully booked");
        }
    }

    public List<Instant> availableSlots(AppointmentType type, LocalDate date) {
        SchedulingSettings settings = currentSettings();
        List<Instant> slots = new ArrayList<>();

        if (!WORKDAYS.contains(date.getDayOfWeek()) || closureRepository.existsByDate(date)) {
            return slots;
        }

        Instant cursor = date.atTime(settings.getBusinessStartTime()).atZone(SHOP_ZONE).toInstant();
        Instant end = date.atTime(settings.getBusinessEndTime()).atZone(SHOP_ZONE).toInstant();

        while (cursor.isBefore(end)) {
            try {
                ensureAvailable(type, cursor);
                slots.add(cursor);
            } catch (SlotUnavailableException ignored) {
                // not offered
            }
            cursor = cursor.plus(Appointment.SLOT_DURATION);
        }

        return slots;
    }

    private Instant alignToSlot(Instant instant) {
        long slotSeconds = Appointment.SLOT_DURATION.getSeconds();
        long epochSeconds = instant.getEpochSecond();
        return Instant.ofEpochSecond(epochSeconds - (epochSeconds % slotSeconds));
    }

    private SchedulingSettings currentSettings() {
        return settingsRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Scheduling settings row is missing"));
    }
}
