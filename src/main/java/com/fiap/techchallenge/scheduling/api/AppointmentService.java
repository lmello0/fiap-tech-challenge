package com.fiap.techchallenge.scheduling.api;

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
import com.fiap.techchallenge.scheduling.api.queries.AppointmentFilterQuery;
import com.fiap.techchallenge.scheduling.api.representation.AppointmentInfo;
import com.fiap.techchallenge.scheduling.api.representation.GuestConversionResult;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    Page<AppointmentInfo> list(AppointmentFilterQuery filter, Pageable pageable);

    AppointmentInfo getById(UUID id);

    AppointmentInfo getByToken(GuestTokenCommand command);

    /** Slot starts still open for the given day, honoring business hours, closures, and capacity. */
    List<Instant> availableSlots(AppointmentType type, LocalDate date);

    AppointmentInfo bookGuestDropoff(BookGuestDropoffCommand command);

    AppointmentInfo bookCustomerDropoff(UUID customerId, BookCustomerDropoffCommand command);

    AppointmentInfo bookDropoffOnBehalf(BookDropoffOnBehalfCommand command);

    AppointmentInfo bookPickup(UUID customerId, BookPickupCommand command);

    AppointmentInfo bookPickupByToken(BookPickupByTokenCommand command);

    /** Attendant/Manager action: marks the Appointment COMPLETED and fires the WorkOrder handoff. */
    AppointmentInfo checkIn(UUID appointmentId);

    AppointmentInfo cancel(UUID appointmentId, CancelAppointmentCommand command);

    /** Authenticated (registered) customer cancelling their own appointment — verifies ownership. */
    AppointmentInfo cancelOwn(UUID appointmentId, UUID customerId, CancelAppointmentCommand command);

    AppointmentInfo cancelByToken(GuestTokenCommand command);

    /** Cancels the original (reason RESCHEDULED, linked) and books a new one for the new slot (ADR 0016). */
    AppointmentInfo reschedule(UUID appointmentId, RescheduleAppointmentCommand command);

    /** Authenticated (registered) customer rescheduling their own appointment — verifies ownership. */
    AppointmentInfo rescheduleOwn(UUID appointmentId, UUID customerId, RescheduleAppointmentCommand command);

    AppointmentInfo rescheduleByToken(GuestRescheduleCommand command);

    GuestConversionResult completeRegistrationViaToken(CompleteGuestRegistrationCommand command);

    GuestConversionResult registerGuestAtCheckIn(UUID appointmentId, RegisterGuestAtCheckInCommand command);
}
