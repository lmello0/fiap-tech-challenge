package com.fiap.techchallenge.history.controllers;

import com.fiap.techchallenge.history.api.representation.HistoryEntryInfo;
import com.fiap.techchallenge.history.api.representation.HistorySnapshotInfo;
import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import com.fiap.techchallenge.shared.responses.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/**
 * Full Swagger/OpenAPI contract for {@link HistoryController} — the generic, staff-only read surface
 * over any aggregate's Timeline. Known {@code aggregateType} values: {@code VEHICLE}, {@code APPOINTMENT},
 * {@code USER}, {@code WORK_ORDER}. For a customer-scoped, ownership-checked read of a work order's
 * history, see {@code CustomerWorkOrderController} instead — this endpoint has no notion of ownership.
 */
@Tag(name = "History", description = "Generic staff read surface over any aggregate's recorded Timeline.")
@RequestMapping("history/{aggregateType}/{aggregateId}")
public interface HistoryControllerSwaggerDoc {

    @Operation(
            summary = "Get an aggregate's history timeline",
            description = "Every recorded milestone for the given aggregate, each usable to fetch its own snapshot. "
                    + "Requires the ATTENDANT, MECHANIC, MANAGER, or STOCKIST role."
    )
    @CommonApiResponses
    @GetMapping
    ResponseEntity<PageResponse<HistoryEntryInfo>> timeline(
            @Parameter(description = "Aggregate type, e.g. VEHICLE, APPOINTMENT, USER, WORK_ORDER") @PathVariable String aggregateType,
            @Parameter(description = "Aggregate id") @PathVariable UUID aggregateId,
            @PageableDefault Pageable pageable
    );

    @Operation(
            summary = "Get a history snapshot",
            description = "The aggregate's full state as it was at the moment one timeline entry was recorded. "
                    + "Requires the ATTENDANT, MECHANIC, MANAGER, or STOCKIST role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No history entry exists with the given entry id for this aggregate.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/entries/{entryId}")
    ResponseEntity<HistorySnapshotInfo> snapshot(
            @Parameter(description = "Aggregate type, e.g. VEHICLE, APPOINTMENT, USER, WORK_ORDER") @PathVariable String aggregateType,
            @Parameter(description = "Aggregate id") @PathVariable UUID aggregateId,
            @Parameter(description = "History entry id") @PathVariable UUID entryId
    );
}
