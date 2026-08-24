package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.exceptions.IllegalPurchaseOrderStateException;
import com.fiap.techchallenge.inventory.exceptions.IllegalReservationTransitionException;
import com.fiap.techchallenge.inventory.exceptions.InsufficientStockException;
import com.fiap.techchallenge.inventory.exceptions.InvalidStockAdjustmentException;
import com.fiap.techchallenge.inventory.exceptions.PartNotFoundException;
import com.fiap.techchallenge.inventory.exceptions.PurchaseOrderNotFoundException;
import com.fiap.techchallenge.inventory.exceptions.RepairServiceNotFoundException;
import com.fiap.techchallenge.inventory.exceptions.StockPolicyNotFoundException;
import com.fiap.techchallenge.inventory.exceptions.VendorNotFoundException;
import com.fiap.techchallenge.shared.exceptions.ExceptionHandlerOrder;
import com.fiap.techchallenge.shared.exceptions.ProblemDetails;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(ExceptionHandlerOrder.MODULE)
public class InventoryExceptionHandler {

    @ExceptionHandler(PartNotFoundException.class)
    ProblemDetail handlePartNotFound(PartNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(RepairServiceNotFoundException.class)
    ProblemDetail handleRepairServiceNotFound(RepairServiceNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(VendorNotFoundException.class)
    ProblemDetail handleVendorNotFound(VendorNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(PurchaseOrderNotFoundException.class)
    ProblemDetail handlePurchaseOrderNotFound(PurchaseOrderNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(StockPolicyNotFoundException.class)
    ProblemDetail handleStockPolicyNotFound(StockPolicyNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(InvalidStockAdjustmentException.class)
    ProblemDetail handleInvalidStockAdjustment(InvalidStockAdjustmentException e) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid stock adjustment", e.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail handleInsufficientStock(InsufficientStockException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Insufficient stock", e.getMessage());
    }

    @ExceptionHandler(IllegalPurchaseOrderStateException.class)
    ProblemDetail handleIllegalPurchaseOrderState(IllegalPurchaseOrderStateException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Illegal purchase order state", e.getMessage());
    }

    @ExceptionHandler(IllegalReservationTransitionException.class)
    ProblemDetail handleIllegalReservationTransition(IllegalReservationTransitionException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Illegal reservation state", e.getMessage());
    }
}
