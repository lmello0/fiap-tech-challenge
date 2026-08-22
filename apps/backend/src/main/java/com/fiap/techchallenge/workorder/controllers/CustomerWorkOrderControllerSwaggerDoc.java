package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.history.api.representation.HistoryEntryInfo;
import com.fiap.techchallenge.history.api.representation.HistorySnapshotInfo;
import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.workorder.api.commands.RefuseWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.representation.BudgetInfo;
import com.fiap.techchallenge.workorder.api.representation.CustomerWorkOrderView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Full Swagger/OpenAPI contract for {@link CustomerWorkOrderController} — the narrow, customer-scoped
 * read of a work order's Budget and status, plus the approve/refuse actions. A CUSTOMER never sees the
 * staff-facing {@link WorkOrderControllerSwaggerDoc} surface.
 */
@Tag(name = "Customer Work Orders", description = "Customer-scoped view of a work order: status, its Budget, and approve/refuse actions.")
@RequestMapping("work-orders/{id}/customer-view")
public interface CustomerWorkOrderControllerSwaggerDoc {

    @Operation(
            summary = "Get a customer's own work order",
            description = "Returns the caller's own work order: high-level status and its Budget, without staff-internal "
                    + "detail. Requires the CUSTOMER role. Returns 404 rather than 403 if the work order belongs to "
                    + "someone else, so as not to reveal whether it exists."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id, or it does not belong to the caller.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    ResponseEntity<CustomerWorkOrderView> getForCustomer(
            @Parameter(description = "Work order id") @PathVariable UUID id,
            Authentication authentication
    );

    @Operation(
            summary = "Approve a sent Budget",
            description = "Approves the Budget that was sent for the caller's own work order, unlocking repair work. "
                    + "Requires the CUSTOMER role. Returns 404 rather than 403 if the work order belongs to someone else."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order or Budget exists with the given id, or it does not belong to the caller.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The Budget is not in a state that allows approval (e.g. not sent, or already resolved).",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/budget/approval")
    ResponseEntity<BudgetInfo> approve(
            @Parameter(description = "Work order id") @PathVariable UUID id,
            @Parameter(description = "The Budget being approved") @RequestParam UUID budgetId,
            Authentication authentication
    );

    @Operation(
            summary = "Refuse a sent Budget",
            description = "Refuses the Budget that was sent for the caller's own work order, with an optional reason. "
                    + "Requires the CUSTOMER role. Returns 404 rather than 403 if the work order belongs to someone else."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order or Budget exists with the given id, or it does not belong to the caller.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The Budget is not in a state that allows refusal (e.g. not sent, or already resolved).",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/budget/refusal")
    ResponseEntity<BudgetInfo> refuse(
            @Parameter(description = "Work order id") @PathVariable UUID id,
            @Parameter(description = "The Budget being refused") @RequestParam UUID budgetId,
            @Valid @RequestBody RefuseWorkOrderCommand command,
            Authentication authentication
    );

    @Operation(
            summary = "Get the customer-visible history timeline",
            description = "Only the timeline entries the owning module marked customer-visible, for the caller's own "
                    + "work order. Requires the CUSTOMER role. Returns 404 rather than 403 if the work order belongs "
                    + "to someone else."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id, or it does not belong to the caller.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/history")
    ResponseEntity<PageResponse<HistoryEntryInfo>> history(
            @Parameter(description = "Work order id") @PathVariable UUID id,
            @PageableDefault Pageable pageable,
            Authentication authentication
    );

    @Operation(
            summary = "Get a customer-visible history snapshot",
            description = "The work order's customer-visible state as it was at the moment one timeline entry was "
                    + "recorded, for the caller's own work order. Requires the CUSTOMER role. Returns 404 rather than "
                    + "403 if the work order belongs to someone else."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id, it does not belong to the caller, or no history entry exists with the given entry id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/history/{entryId}")
    ResponseEntity<HistorySnapshotInfo> historyEntry(
            @Parameter(description = "Work order id") @PathVariable UUID id,
            @Parameter(description = "History entry id") @PathVariable UUID entryId,
            Authentication authentication
    );
}
