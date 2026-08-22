package com.fiap.techchallenge.scheduling.services;

import com.fiap.techchallenge.auth.api.AuthService;
import com.fiap.techchallenge.auth.api.commands.RegisterCustomerCommand;
import com.fiap.techchallenge.auth.api.representation.TemporaryCredential;
import com.fiap.techchallenge.scheduling.api.commands.CompleteGuestRegistrationCommand;
import com.fiap.techchallenge.scheduling.api.commands.RegisterGuestAtCheckInCommand;
import com.fiap.techchallenge.scheduling.api.representation.GuestConversionResult;
import com.fiap.techchallenge.scheduling.entities.Appointment;
import com.fiap.techchallenge.scheduling.enums.AppointmentCancelReason;
import com.fiap.techchallenge.scheduling.exceptions.AppointmentNotFoundException;
import com.fiap.techchallenge.scheduling.exceptions.IllegalAppointmentStateException;
import com.fiap.techchallenge.scheduling.mappers.AppointmentMapper;
import com.fiap.techchallenge.scheduling.repositories.AppointmentRepository;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import com.fiap.techchallenge.user.api.commands.RegisterPhoneNumberCommand;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.user.enums.PhoneType;
import com.fiap.techchallenge.vehicle.api.VehicleService;
import com.fiap.techchallenge.vehicle.api.commands.CreateVehicleCommand;
import com.fiap.techchallenge.vehicle.api.representation.VehicleInfo;
import com.fiap.techchallenge.vehicle.enums.FuelType;
import com.fiap.techchallenge.vehicle.enums.TransmissionType;
import com.fiap.techchallenge.vehicle.enums.VehicleType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Guest Conversion (CONTEXT.md): turning a Guest's captured Appointment details into a real
 * User+Customer+Vehicle. Two entry points — self-service via the Complete-Registration Token, and
 * Attendant-initiated at Check-in — that otherwise do the same thing (ADR 0015): neither creates a
 * shadow account before this point, so both need the same extra fields the original guest form never
 * collected (document, license plate, and the rest of what {@code CreateVehicleCommand} requires).
 */
@Service
@RequiredArgsConstructor
public class GuestConversionService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentTokenService tokenService;
    private final AppointmentMapper appointmentMapper;
    private final AuthService authService;
    private final UserService userService;
    private final VehicleService vehicleService;

    @Transactional
    public GuestConversionResult completeRegistrationViaToken(CompleteGuestRegistrationCommand command) {
        UUID appointmentId = tokenService.consumeRegistrationToken(command.token());
        Appointment appointment = resolveLive(findOrThrow(appointmentId));

        requireGuest(appointment);

        CreateUserCommand userCommand = buildUserCommand(appointment, command.documentType(), command.documentCode());
        authService.registerCustomer(new RegisterCustomerCommand(userCommand, command.rawPassword()));

        UserInfo user = userService.findByEmail(userCommand.email())
                .orElseThrow(() -> new IllegalStateException("User vanished right after registration"));

        VehicleInfo vehicle = vehicleService.create(buildVehicleCommand(
                appointment, user.id(), command.licensePlate(), command.vehicleType(),
                command.color(), command.fuelType(), command.transmissionType()));

        appointment.completeGuestConversion(user.id(), vehicle.id());
        appointmentRepository.save(appointment);

        return new GuestConversionResult(appointmentMapper.toInfo(appointment), null);
    }

    @Transactional
    public GuestConversionResult registerGuestAtCheckIn(UUID appointmentId, RegisterGuestAtCheckInCommand command) {
        Appointment appointment = resolveLive(findOrThrow(appointmentId));

        requireGuest(appointment);

        CreateUserCommand userCommand = buildUserCommand(appointment, command.documentType(), command.documentCode());
        TemporaryCredential credential = authService.registerCustomerWithTemporaryPassword(userCommand);

        VehicleInfo vehicle = vehicleService.create(buildVehicleCommand(
                appointment, credential.user().id(), command.licensePlate(), command.vehicleType(),
                command.color(), command.fuelType(), command.transmissionType()));

        appointment.completeGuestConversion(credential.user().id(), vehicle.id());
        appointmentRepository.save(appointment);

        return new GuestConversionResult(appointmentMapper.toInfo(appointment), credential.rawPassword());
    }

    private void requireGuest(Appointment appointment) {
        if (!appointment.isGuest()) {
            throw new IllegalAppointmentStateException(appointment.getId(), appointment.getStatus(), "convert (already registered)");
        }
    }

    private CreateUserCommand buildUserCommand(Appointment appointment, DocumentType documentType, String documentCode) {
        String fullName = appointment.getGuestName().trim();
        int spaceIndex = fullName.indexOf(' ');
        String firstName = spaceIndex < 0 ? fullName : fullName.substring(0, spaceIndex);
        String lastName = spaceIndex < 0 ? null : fullName.substring(spaceIndex + 1).trim();

        return new CreateUserCommand(
                appointment.getGuestEmail(),
                firstName,
                lastName,
                documentType,
                documentCode,
                List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, appointment.getGuestPhone(), true))
        );
    }

    private CreateVehicleCommand buildVehicleCommand(
            Appointment appointment, UUID customerId, String licensePlate, VehicleType vehicleType,
            String color, FuelType fuelType, TransmissionType transmissionType
    ) {
        return new CreateVehicleCommand(
                customerId,
                vehicleType,
                licensePlate,
                appointment.getGuestVehicleMake(),
                appointment.getGuestVehicleModel(),
                color,
                appointment.getGuestVehicleYear(),
                null,
                null,
                fuelType,
                transmissionType
        );
    }

    private Appointment findOrThrow(UUID id) {
        return appointmentRepository.findById(id).orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    /**
     * A registration link mailed before a Reschedule still points at the original Appointment id;
     * follow the RESCHEDULED chain (ADR 0016) to whichever Appointment is actually still live.
     */
    private Appointment resolveLive(Appointment appointment) {
        Appointment current = appointment;
        int hops = 0;

        while (current.getCancelReason() == AppointmentCancelReason.RESCHEDULED
                && current.getRescheduledToId() != null && hops++ < 10) {
            Appointment next = appointmentRepository.findById(current.getRescheduledToId()).orElse(null);
            if (next == null || next.getId().equals(appointment.getId())) {
                break;
            }
            current = next;
        }

        return current;
    }
}
