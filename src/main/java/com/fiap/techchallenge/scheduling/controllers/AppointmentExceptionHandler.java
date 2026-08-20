package com.fiap.techchallenge.scheduling.controllers;

import com.fiap.techchallenge.scheduling.exceptions.AppointmentNotFoundException;
import com.fiap.techchallenge.scheduling.exceptions.ClosureConflictException;
import com.fiap.techchallenge.scheduling.exceptions.GuestBookingLimitExceededException;
import com.fiap.techchallenge.scheduling.exceptions.IllegalAppointmentStateException;
import com.fiap.techchallenge.scheduling.exceptions.InvalidAppointmentTokenException;
import com.fiap.techchallenge.scheduling.exceptions.InvalidBookingRequestException;
import com.fiap.techchallenge.scheduling.exceptions.SlotUnavailableException;
import com.fiap.techchallenge.scheduling.exceptions.VehicleOwnershipException;
import com.fiap.techchallenge.shared.exceptions.ExceptionHandlerOrder;
import com.fiap.techchallenge.shared.exceptions.ProblemDetails;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(ExceptionHandlerOrder.MODULE)
public class AppointmentExceptionHandler {

    @ExceptionHandler(AppointmentNotFoundException.class)
    ProblemDetail handleNotFound(AppointmentNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(InvalidAppointmentTokenException.class)
    ProblemDetail handleInvalidToken(InvalidAppointmentTokenException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Invalid token", e.getMessage());
    }

    @ExceptionHandler(SlotUnavailableException.class)
    ProblemDetail handleSlotUnavailable(SlotUnavailableException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Slot unavailable", e.getMessage());
    }

    @ExceptionHandler(GuestBookingLimitExceededException.class)
    ProblemDetail handleGuestLimit(GuestBookingLimitExceededException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Booking limit reached", e.getMessage());
    }

    @ExceptionHandler(IllegalAppointmentStateException.class)
    ProblemDetail handleIllegalState(IllegalAppointmentStateException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Illegal appointment state", e.getMessage());
    }

    @ExceptionHandler(ClosureConflictException.class)
    ProblemDetail handleClosureConflict(ClosureConflictException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Closure conflict", e.getMessage());
    }

    @ExceptionHandler(InvalidBookingRequestException.class)
    ProblemDetail handleInvalidBookingRequest(InvalidBookingRequestException e) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "Invalid booking request", e.getMessage());
    }

    @ExceptionHandler(VehicleOwnershipException.class)
    ProblemDetail handleVehicleOwnership(VehicleOwnershipException e) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "Vehicle does not belong to customer", e.getMessage());
    }
}
