package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.history.api.representation.HistoryEntryInfo;
import com.fiap.techchallenge.history.api.representation.HistorySnapshotInfo;
import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.workorder.api.commands.CreateWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.commands.FinishDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.commands.StartDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.queries.WorkOrderFilterQuery;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderCountStatusInfo;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
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
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Full Swagger/OpenAPI contract for {@link WorkOrderController} — the staff-facing work order
 * lifecycle, from creation through diagnostics, repair execution, and delivery. See
 * {@link CustomerWorkOrderControllerSwaggerDoc} for the narrower customer-scoped surface.
 */
@Tag(name = "Work Orders", description = "Staff-facing work order lifecycle: creation, diagnostics, repair execution, and delivery.")
@RequestMapping("work-orders")
public interface WorkOrderControllerSwaggerDoc {

    @Operation(
            summary = "Count the work orders by statuses",
            description = "Returns the count of work orders in each status, can be filtered by date"
    )
    @CommonApiResponses
    @GetMapping("/summary")
    ResponseEntity<WorkOrderCountStatusInfo> getWorkOrderStatusInfo(
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end
    );

    @Operation(
            summary = "List work orders",
            description = "Returns a paginated, filterable list of work orders. Requires the ATTENDANT, MECHANIC, or MANAGER role."
    )
    @CommonApiResponses
    @GetMapping
    ResponseEntity<PageResponse<WorkOrderInfo>> getAll(
            @PageableDefault Pageable pageable,
            @Valid WorkOrderFilterQuery filter
    );

    @Operation(
            summary = "Get a work order by id",
            description = "Requires the ATTENDANT, MECHANIC, or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<WorkOrderInfo> getById(@Parameter(description = "Work order id") @PathVariable UUID id);

    @Operation(
            summary = "Open a work order",
            description = "Creates a new work order for a customer's vehicle, starting in status RECEIVED. "
                    + "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "The referenced customer or vehicle does not exist.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<WorkOrderInfo> create(@Valid @RequestBody CreateWorkOrderCommand woCommand);

    @Operation(
            summary = "Request diagnostics",
            description = "Moves the work order into the diagnostics-requested state, queuing it for a mechanic to pick up. "
                    + "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The work order is not in a state that allows requesting diagnostics.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/diagnostics/request")
    ResponseEntity<WorkOrderInfo> requestDiagnostics(@Parameter(description = "Work order id") @PathVariable UUID id);

    @Operation(
            summary = "Start diagnostics",
            description = "Assigns the given mechanic and marks diagnostics as in progress. "
                    + "Requires the MECHANIC or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The work order is not in a state that allows starting diagnostics.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/diagnostics/start")
    ResponseEntity<WorkOrderInfo> startDiagnostics(
            @Parameter(description = "Work order id") @PathVariable UUID id,
            @Valid @RequestBody StartDiagnosticsCommand command
    );

    @Operation(
            summary = "Finish diagnostics",
            description = "Records the diagnosis and atomically drafts the work order's Budget, seeded with the given lines. "
                    + "Requires the MECHANIC or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id, or a referenced part/service does not exist.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The work order is not in a state that allows finishing diagnostics.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/diagnostics/finish")
    ResponseEntity<WorkOrderInfo> finishDiagnostics(
            @Parameter(description = "Work order id") @PathVariable UUID id,
            @Valid @RequestBody FinishDiagnosticsCommand command
    );

    @Operation(
            summary = "Start repair service",
            description = "Marks repair work as started once the customer has approved the Budget. "
                    + "Requires the MECHANIC or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The work order is not in a state that allows starting service.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/service/start")
    ResponseEntity<WorkOrderInfo> start(@Parameter(description = "Work order id") @PathVariable UUID id);

    @Operation(
            summary = "Finish repair service",
            description = "Marks repair work as finished, once every Budget line has been finished. "
                    + "Requires the MECHANIC or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The work order is not in a state that allows finishing service, or a Budget line is still open.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/service/finish")
    ResponseEntity<WorkOrderInfo> finish(@Parameter(description = "Work order id") @PathVariable UUID id);

    @Operation(
            summary = "Start a Budget line",
            description = "Marks a single approved Budget line as started. Requires the MECHANIC or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order or Budget line exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The work order or the line is not in a state that allows starting it.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/lines/{lineId}/start")
    ResponseEntity<WorkOrderInfo> startLine(
            @Parameter(description = "Work order id") @PathVariable UUID id,
            @Parameter(description = "Budget line id") @PathVariable UUID lineId
    );

    @Operation(
            summary = "Finish a Budget line",
            description = "Marks a single Budget line as finished. Requires the MECHANIC or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order or Budget line exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The line is not in a state that allows finishing it.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/lines/{lineId}/finish")
    ResponseEntity<WorkOrderInfo> finishLine(
            @Parameter(description = "Work order id") @PathVariable UUID id,
            @Parameter(description = "Budget line id") @PathVariable UUID lineId
    );

    @Operation(
            summary = "Mark ready for pickup",
            description = "Marks the vehicle as ready for the customer to collect. Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The work order is not in a state that allows marking it ready for pickup.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/pickup-ready")
    ResponseEntity<WorkOrderInfo> pickupReady(@Parameter(description = "Work order id") @PathVariable UUID id);

    @Operation(
            summary = "Record delivery",
            description = "Marks the vehicle as delivered back to the customer, closing the work order. "
                    + "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The work order is not in a state that allows recording delivery.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/delivery")
    ResponseEntity<WorkOrderInfo> delivery(@Parameter(description = "Work order id") @PathVariable UUID id);

    @Operation(
            summary = "Get the work order's history timeline",
            description = "Every recorded milestone for this work order, each usable to fetch its own snapshot. "
                    + "Requires the ATTENDANT, MECHANIC, or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}/history")
    ResponseEntity<PageResponse<HistoryEntryInfo>> history(
            @Parameter(description = "Work order id") @PathVariable UUID id,
            @PageableDefault Pageable pageable
    );

    @Operation(
            summary = "Get a history snapshot",
            description = "The work order's full state as it was at the moment one timeline entry was recorded. "
                    + "Requires the ATTENDANT, MECHANIC, or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No work order exists with the given id, or no history entry exists with the given entry id for it.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}/history/{entryId}")
    ResponseEntity<HistorySnapshotInfo> historyEntry(
            @Parameter(description = "Work order id") @PathVariable UUID id,
            @Parameter(description = "History entry id") @PathVariable UUID entryId
    );
}
