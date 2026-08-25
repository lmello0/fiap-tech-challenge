package com.fiap.techchallenge.scheduling.services;

import com.fiap.techchallenge.scheduling.api.AppointmentService;
import com.fiap.techchallenge.scheduling.api.commands.BookCustomerDropoffCommand;
import com.fiap.techchallenge.scheduling.api.commands.BookDropoffOnBehalfCommand;
import com.fiap.techchallenge.scheduling.api.commands.BookGuestDropoffCommand;
import com.fiap.techchallenge.scheduling.api.commands.BookPickupByTokenCommand;
import com.fiap.techchallenge.scheduling.api.commands.BookPickupCommand;
import com.fiap.techchallenge.scheduling.api.commands.CancelAppointmentCommand;
import com.fiap.techchallenge.scheduling.api.commands.CompleteGuestRegistrationCommand;
import com.fiap.techchallenge.scheduling.api.commands.GuestRescheduleCommand;
import com.fiap.techchallenge.scheduling.api.commands.GuestTokenCommand;
import com.fiap.techchallenge.scheduling.api.commands.RegisterGuestAtCheckInCommand;
import com.fiap.techchallenge.scheduling.api.commands.RescheduleAppointmentCommand;
import com.fiap.techchallenge.scheduling.api.events.AppointmentBookedEvent;
import com.fiap.techchallenge.scheduling.api.events.AppointmentCancelledEvent;
import com.fiap.techchallenge.scheduling.api.events.AppointmentCheckedInEvent;
import com.fiap.techchallenge.scheduling.api.queries.AppointmentFilterQuery;
import com.fiap.techchallenge.scheduling.api.representation.AppointmentInfo;
import com.fiap.techchallenge.scheduling.api.representation.AppointmentSnapshot;
import com.fiap.techchallenge.scheduling.api.representation.GuestConversionResult;
import com.fiap.techchallenge.scheduling.entities.Appointment;
import com.fiap.techchallenge.scheduling.entities.PickupInvitationToken;
import com.fiap.techchallenge.scheduling.enums.AppointmentCancelReason;
import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import com.fiap.techchallenge.scheduling.exceptions.AppointmentNotFoundException;
import com.fiap.techchallenge.scheduling.exceptions.GuestBookingLimitExceededException;
import com.fiap.techchallenge.scheduling.exceptions.GuestEmailAlreadyRegisteredException;
import com.fiap.techchallenge.scheduling.exceptions.IllegalAppointmentStateException;
import com.fiap.techchallenge.scheduling.exceptions.InvalidBookingRequestException;
import com.fiap.techchallenge.scheduling.exceptions.VehicleOwnershipException;
import com.fiap.techchallenge.scheduling.mappers.AppointmentMapper;
import com.fiap.techchallenge.scheduling.notifications.SchedulingEmails;
import com.fiap.techchallenge.scheduling.repositories.AppointmentRepository;
import com.fiap.techchallenge.scheduling.repositories.specifications.AppointmentSpecifications;
import com.fiap.techchallenge.shared.audit.ActorResolver;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.vehicle.api.VehicleService;
import com.fiap.techchallenge.vehicle.api.representation.VehicleInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository repository;
    private final AppointmentMapper mapper;
    private final AvailabilityService availabilityService;
    private final AppointmentTokenService tokenService;
    private final SchedulingEmails emails;
    private final GuestConversionService guestConversionService;
    private final VehicleService vehicleService;
    private final UserService userService;
    private final ApplicationEventPublisher events;
    private final ActorResolver actorResolver;

    @Override
    public Page<AppointmentInfo> list(AppointmentFilterQuery filter, Pageable pageable) {
        Specification<Appointment> spec = Specification
                .allOf(
                        AppointmentSpecifications.ofType(filter.type()),
                        AppointmentSpecifications.withStatus(filter.status()),
                        AppointmentSpecifications.belongsToCustomer(filter.customerId()),
                        AppointmentSpecifications.ofWorkOrder(filter.workOrderId()),
                        AppointmentSpecifications.onDate(filter.date())
                );

        return repository.findAll(spec, pageable).map(mapper::toInfo);
    }

    @Override
    public AppointmentInfo getById(UUID id) {
        return mapper.toInfo(findOrThrow(id));
    }

    @Override
    public AppointmentInfo getByToken(GuestTokenCommand command) {
        UUID appointmentId = tokenService.resolveAccessToken(command.token());
        return mapper.toInfo(findOrThrow(appointmentId));
    }

    @Override
    public List<Instant> availableSlots(AppointmentType type, LocalDate date) {
        return availabilityService.availableSlots(type, date);
    }

    @Override
    @Transactional
    public AppointmentInfo bookGuestDropoff(BookGuestDropoffCommand command) {
        ensureEmailNotRegistered(command.guestEmail());
        ensureGuestNotAlreadyBooked(command.guestPhone(), command.guestEmail());
        availabilityService.ensureAvailable(AppointmentType.DROPOFF, command.slotStart());

        Appointment appointment = new Appointment();
        appointment.setType(AppointmentType.DROPOFF);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setSlotStart(command.slotStart());
        appointment.setGuestName(command.guestName());
        appointment.setGuestPhone(command.guestPhone());
        appointment.setGuestEmail(command.guestEmail());
        appointment.setGuestVehicleMake(command.guestVehicleMake());
        appointment.setGuestVehicleModel(command.guestVehicleModel());
        appointment.setGuestVehicleYear(command.guestVehicleYear());
        appointment.setComplaint(command.complaint());

        repository.save(appointment);
        publishBooked(appointment);
        sendGuestBookingConfirmation(appointment);

        return mapper.toInfo(appointment);
    }

    @Override
    @Transactional
    public AppointmentInfo bookCustomerDropoff(UUID customerId, BookCustomerDropoffCommand command) {
        assertOwnsVehicle(customerId, command.vehicleId());
        availabilityService.ensureAvailable(AppointmentType.DROPOFF, command.slotStart());

        Appointment appointment = new Appointment();
        appointment.setType(AppointmentType.DROPOFF);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setSlotStart(command.slotStart());
        appointment.setCustomerId(customerId);
        appointment.setVehicleId(command.vehicleId());
        appointment.setComplaint(command.complaint());

        repository.save(appointment);
        publishBooked(appointment);

        return mapper.toInfo(appointment);
    }

    @Override
    @Transactional
    public AppointmentInfo bookDropoffOnBehalf(BookDropoffOnBehalfCommand command) {
        boolean hasCustomer = command.customerId() != null;
        boolean hasGuest = command.guestName() != null || command.guestPhone() != null || command.guestEmail() != null;

        if (hasCustomer == hasGuest) {
            throw new InvalidBookingRequestException(
                    "Supply either an existing customerId + vehicleId, or guest contact details — not both, not neither");
        }

        availabilityService.ensureAvailable(AppointmentType.DROPOFF, command.slotStart());

        Appointment appointment = new Appointment();
        appointment.setType(AppointmentType.DROPOFF);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setSlotStart(command.slotStart());
        appointment.setComplaint(command.complaint());

        if (hasCustomer) {
            if (command.vehicleId() == null) {
                throw new InvalidBookingRequestException("vehicleId is required when booking for an existing customer");
            }

            assertOwnsVehicle(command.customerId(), command.vehicleId());
            appointment.setCustomerId(command.customerId());
            appointment.setVehicleId(command.vehicleId());
        } else {
            ensureGuestNotAlreadyBooked(command.guestPhone(), command.guestEmail());
            appointment.setGuestName(command.guestName());
            appointment.setGuestPhone(command.guestPhone());
            appointment.setGuestEmail(command.guestEmail());
            appointment.setGuestVehicleMake(command.guestVehicleMake());
            appointment.setGuestVehicleModel(command.guestVehicleModel());
            appointment.setGuestVehicleYear(command.guestVehicleYear());
        }

        repository.save(appointment);
        publishBooked(appointment);

        if (appointment.isGuest()) {
            sendGuestBookingConfirmation(appointment);
        }

        return mapper.toInfo(appointment);
    }

    @Override
    @Transactional
    public AppointmentInfo bookPickup(UUID customerId, BookPickupCommand command) {
        assertOwnsVehicle(customerId, command.vehicleId());

        return bookPickupInternal(customerId, command.vehicleId(), command.workOrderId(), command.slotStart());
    }

    @Override
    @Transactional
    public AppointmentInfo bookPickupByToken(BookPickupByTokenCommand command) {
        PickupInvitationToken invitation = tokenService.consumePickupInvitationToken(command.token());

        return bookPickupInternal(
                invitation.getCustomerId(), invitation.getVehicleId(), invitation.getWorkOrderId(), command.slotStart());
    }

    @Override
    @Transactional
    public AppointmentInfo checkIn(UUID appointmentId) {
        Appointment appointment = findOrThrow(appointmentId);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new IllegalAppointmentStateException(appointmentId, appointment.getStatus(), "check in");
        }

        appointment.checkIn();
        repository.save(appointment);

        events.publishEvent(new AppointmentCheckedInEvent(
                appointment.getId(),
                appointment.getType(),
                appointment.getCustomerId(),
                appointment.getVehicleId(),
                appointment.getWorkOrderId(),
                appointment.getComplaint(),
                actorResolver.forCurrentUser(true),
                snapshot(appointment)
        ));

        return mapper.toInfo(appointment);
    }

    @Override
    @Transactional
    public AppointmentInfo cancel(UUID appointmentId, CancelAppointmentCommand command) {
        return doCancel(findOrThrow(appointmentId), AppointmentCancelReason.STAFF_REQUESTED, command.message());
    }

    @Override
    @Transactional
    public AppointmentInfo cancelOwn(UUID appointmentId, UUID customerId, CancelAppointmentCommand command) {
        Appointment appointment = findOrThrow(appointmentId);
        assertOwnsAppointment(appointment, customerId);
        return doCancel(appointment, AppointmentCancelReason.CUSTOMER_REQUESTED, command.message());
    }

    @Override
    @Transactional
    public AppointmentInfo cancelByToken(GuestTokenCommand command) {
        UUID appointmentId = tokenService.resolveAccessToken(command.token());
        return doCancel(findOrThrow(appointmentId), AppointmentCancelReason.CUSTOMER_REQUESTED, null);
    }

    @Override
    @Transactional
    public AppointmentInfo reschedule(UUID appointmentId, RescheduleAppointmentCommand command) {
        return doReschedule(findOrThrow(appointmentId), command.newSlotStart());
    }

    @Override
    @Transactional
    public AppointmentInfo rescheduleOwn(UUID appointmentId, UUID customerId, RescheduleAppointmentCommand command) {
        Appointment appointment = findOrThrow(appointmentId);
        assertOwnsAppointment(appointment, customerId);
        return doReschedule(appointment, command.newSlotStart());
    }

    @Override
    @Transactional
    public AppointmentInfo rescheduleByToken(GuestRescheduleCommand command) {
        UUID appointmentId = tokenService.resolveAccessToken(command.token());
        return doReschedule(findOrThrow(appointmentId), command.newSlotStart());
    }

    @Override
    @Transactional
    public GuestConversionResult completeRegistrationViaToken(CompleteGuestRegistrationCommand command) {
        return guestConversionService.completeRegistrationViaToken(command);
    }

    @Override
    @Transactional
    public GuestConversionResult registerGuestAtCheckIn(UUID appointmentId, RegisterGuestAtCheckInCommand command) {
        return guestConversionService.registerGuestAtCheckIn(appointmentId, command);
    }

    private AppointmentInfo bookPickupInternal(UUID customerId, UUID vehicleId, UUID workOrderId, Instant slotStart) {
        availabilityService.ensureAvailable(AppointmentType.PICKUP, slotStart);

        Appointment appointment = new Appointment();
        appointment.setType(AppointmentType.PICKUP);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setSlotStart(slotStart);
        appointment.setCustomerId(customerId);
        appointment.setVehicleId(vehicleId);
        appointment.setWorkOrderId(workOrderId);

        repository.save(appointment);
        publishBooked(appointment);

        return mapper.toInfo(appointment);
    }

    private AppointmentInfo doCancel(Appointment appointment, AppointmentCancelReason reason, String message) {
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new IllegalAppointmentStateException(appointment.getId(), appointment.getStatus(), "cancel");
        }

        appointment.cancel(reason, message);
        repository.save(appointment);

        events.publishEvent(new AppointmentCancelledEvent(
                appointment.getId(), reason, actorResolver.forCurrentUser(true), snapshot(appointment)));

        if (appointment.isGuest()) {
            emails.appointmentCancelled(appointment.getGuestEmail(), message);
        }

        return mapper.toInfo(appointment);
    }

    private AppointmentInfo doReschedule(Appointment original, Instant newSlotStart) {
        if (original.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new IllegalAppointmentStateException(original.getId(), original.getStatus(), "reschedule");
        }

        availabilityService.ensureAvailable(original.getType(), newSlotStart);

        Appointment replacement = new Appointment();
        replacement.setType(original.getType());
        replacement.setStatus(AppointmentStatus.SCHEDULED);
        replacement.setSlotStart(newSlotStart);
        replacement.setCustomerId(original.getCustomerId());
        replacement.setVehicleId(original.getVehicleId());
        replacement.setGuestName(original.getGuestName());
        replacement.setGuestPhone(original.getGuestPhone());
        replacement.setGuestEmail(original.getGuestEmail());
        replacement.setGuestVehicleMake(original.getGuestVehicleMake());
        replacement.setGuestVehicleModel(original.getGuestVehicleModel());
        replacement.setGuestVehicleYear(original.getGuestVehicleYear());
        replacement.setComplaint(original.getComplaint());
        replacement.setWorkOrderId(original.getWorkOrderId());

        repository.save(replacement);
        publishBooked(replacement);

        original.cancel(AppointmentCancelReason.RESCHEDULED, null);
        original.linkReschedule(replacement.getId());
        repository.save(original);

        events.publishEvent(new AppointmentCancelledEvent(
                original.getId(), AppointmentCancelReason.RESCHEDULED, actorResolver.forCurrentUser(true), snapshot(original)));

        if (replacement.isGuest()) {
            sendGuestBookingConfirmation(replacement);
        }

        return mapper.toInfo(replacement);
    }

    private void sendGuestBookingConfirmation(Appointment appointment) {
        String accessToken = tokenService.issueAccessToken(appointment.getId());
        String registrationToken = tokenService.issueRegistrationToken(appointment.getId());
        emails.dropoffBookingConfirmed(appointment.getGuestEmail(), accessToken, registrationToken);
    }

    private void ensureGuestNotAlreadyBooked(String phone, String email) {
        if (repository.existsActiveDropoffForGuestContact(phone, email)) {
            throw new GuestBookingLimitExceededException();
        }
    }

    /**
     * A Guest is by definition unregistered (CONTEXT.md) — booking with an email that already
     * belongs to a real User would otherwise mail that stranger a "finish setting up your account"
     * link (a phishing-shaped lure) and later hard-fail Guest Conversion at the check-in desk with an
     * error an Attendant can't clear.
     */
    private void ensureEmailNotRegistered(String email) {
        if (userService.findByEmail(email).isPresent()) {
            throw new GuestEmailAlreadyRegisteredException();
        }
    }

    private void assertOwnsAppointment(Appointment appointment, UUID customerId) {
        if (!customerId.equals(appointment.getCustomerId())) {
            throw new AppointmentNotFoundException(appointment.getId());
        }
    }

    private void assertOwnsVehicle(UUID customerId, UUID vehicleId) {
        VehicleInfo vehicle = vehicleService.getById(vehicleId);
        if (!vehicle.customerId().equals(customerId)) {
            throw new VehicleOwnershipException(vehicleId, customerId);
        }
    }

    private void publishBooked(Appointment appointment) {
        events.publishEvent(new AppointmentBookedEvent(
                appointment.getId(), appointment.getType(), actorResolver.forCurrentUser(true), snapshot(appointment)));
    }

    private AppointmentSnapshot snapshot(Appointment appointment) {
        return new AppointmentSnapshot(mapper.toInfo(appointment));
    }

    private Appointment findOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new AppointmentNotFoundException(id));
    }
}
