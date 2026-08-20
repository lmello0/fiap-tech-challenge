package com.fiap.techchallenge.scheduling.services;

import com.fiap.techchallenge.scheduling.api.SchedulingSettingsService;
import com.fiap.techchallenge.scheduling.api.commands.CreateClosureCommand;
import com.fiap.techchallenge.scheduling.api.commands.UpdateSchedulingSettingsCommand;
import com.fiap.techchallenge.scheduling.api.events.AppointmentCancelledEvent;
import com.fiap.techchallenge.scheduling.api.representation.AppointmentSnapshot;
import com.fiap.techchallenge.scheduling.api.representation.ClosureInfo;
import com.fiap.techchallenge.scheduling.api.representation.SchedulingSettingsInfo;
import com.fiap.techchallenge.scheduling.entities.Appointment;
import com.fiap.techchallenge.scheduling.entities.Closure;
import com.fiap.techchallenge.scheduling.entities.SchedulingSettings;
import com.fiap.techchallenge.scheduling.enums.AppointmentCancelReason;
import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;
import com.fiap.techchallenge.scheduling.exceptions.ClosureConflictException;
import com.fiap.techchallenge.scheduling.mappers.AppointmentMapper;
import com.fiap.techchallenge.scheduling.mappers.ClosureMapper;
import com.fiap.techchallenge.scheduling.mappers.SchedulingSettingsMapper;
import com.fiap.techchallenge.scheduling.notifications.SchedulingEmails;
import com.fiap.techchallenge.scheduling.repositories.AppointmentRepository;
import com.fiap.techchallenge.scheduling.repositories.ClosureRepository;
import com.fiap.techchallenge.scheduling.repositories.SchedulingSettingsRepository;
import com.fiap.techchallenge.shared.audit.ActorResolver;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * The Operating Calendar's Manager-editable side (CONTEXT.md). Creating a Closure on a date with
 * existing SCHEDULED appointments auto-cancels them and notifies each affected Customer/Guest,
 * carrying the Manager's optional message.
 */
@Service
@RequiredArgsConstructor
public class SchedulingSettingsServiceImpl implements SchedulingSettingsService {

    private static final ZoneId SHOP_ZONE = ZoneId.systemDefault();

    private final SchedulingSettingsRepository settingsRepository;
    private final SchedulingSettingsMapper settingsMapper;
    private final ClosureRepository closureRepository;
    private final ClosureMapper closureMapper;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final SchedulingEmails emails;
    private final ApplicationEventPublisher events;
    private final ActorResolver actorResolver;
    private final UserService userService;

    @Override
    public SchedulingSettingsInfo getSettings() {
        return settingsMapper.toInfo(currentSettings());
    }

    @Override
    @Transactional
    public SchedulingSettingsInfo updateSettings(UpdateSchedulingSettingsCommand command) {
        SchedulingSettings settings = currentSettings();
        settings.setBusinessStartTime(command.businessStartTime());
        settings.setBusinessEndTime(command.businessEndTime());
        settings.setDropoffSlotCapacity(command.dropoffSlotCapacity());
        settings.setPickupSlotCapacity(command.pickupSlotCapacity());

        settingsRepository.save(settings);

        return settingsMapper.toInfo(settings);
    }

    @Override
    public List<ClosureInfo> listClosures() {
        return closureRepository.findAllByOrderByDateAsc().stream().map(closureMapper::toInfo).toList();
    }

    @Override
    @Transactional
    public ClosureInfo createClosure(CreateClosureCommand command) {
        if (closureRepository.existsByDate(command.date())) {
            throw new ClosureConflictException(command.date());
        }

        Closure closure = new Closure();
        closure.setDate(command.date());
        closure.setMessage(command.message());
        closureRepository.save(closure);

        cancelAppointmentsOnClosedDate(command.date(), command.message());

        return closureMapper.toInfo(closure);
    }

    @Override
    @Transactional
    public void deleteClosure(LocalDate date) {
        closureRepository.findByDate(date).ifPresent(closureRepository::delete);
    }

    private void cancelAppointmentsOnClosedDate(LocalDate date, String message) {
        Instant start = date.atStartOfDay(SHOP_ZONE).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(SHOP_ZONE).toInstant();

        List<Appointment> affected = appointmentRepository
                .findByStatusAndSlotStartGreaterThanEqualAndSlotStartLessThan(AppointmentStatus.SCHEDULED, start, end);

        for (Appointment appointment : affected) {
            appointment.cancel(AppointmentCancelReason.MANAGER_CLOSURE, message);
            appointmentRepository.save(appointment);

            events.publishEvent(new AppointmentCancelledEvent(
                    appointment.getId(),
                    AppointmentCancelReason.MANAGER_CLOSURE,
                    actorResolver.forCurrentUser(true),
                    new AppointmentSnapshot(appointmentMapper.toInfo(appointment))
            ));

            notifyAffected(appointment, message);
        }
    }

    private void notifyAffected(Appointment appointment, String message) {
        String contactEmail = appointment.isGuest()
                ? appointment.getGuestEmail()
                : userService.findById(appointment.getCustomerId()).map(UserInfo::email).orElse(null);

        if (contactEmail != null) {
            emails.appointmentCancelled(contactEmail, message);
        }
    }

    private SchedulingSettings currentSettings() {
        return settingsRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Scheduling settings row is missing"));
    }
}
