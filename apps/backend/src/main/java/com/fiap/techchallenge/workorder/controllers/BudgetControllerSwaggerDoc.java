package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import com.fiap.techchallenge.workorder.api.commands.AddBudgetLineCommand;
import com.fiap.techchallenge.workorder.api.commands.BudgetTokenCommand;
import com.fiap.techchallenge.workorder.api.commands.BudgetTokenRefusalCommand;
import com.fiap.techchallenge.workorder.api.commands.ChangeBudgetLineQuantityCommand;
import com.fiap.techchallenge.workorder.api.representation.BudgetInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/**
 * Full Swagger/OpenAPI contract for {@link BudgetController} — staff-only Budget draft editing and
 * send lifecycle (ADR 0008/0009/0010), plus the public {@code /decision/*} endpoints a customer
 * reaches from the Budget email via a token (ADR 0021) instead of a JWT.
 */
@Tag(name = "Budgets", description = "Staff Budget draft editing and send lifecycle, plus the public token-based decision endpoints.")
@RequestMapping("budgets")
public interface BudgetControllerSwaggerDoc {

    @Operation(
            summary = "Get a Budget by id",
            description = "Requires the ATTENDANT, MECHANIC, or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No Budget exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<BudgetInfo> getById(@Parameter(description = "Budget id") @PathVariable UUID id);

    @Operation(
            summary = "Add a Budget line",
            description = "Adds a part or service line to a Budget still in DRAFT. Description and unit price are "
                    + "snapshotted from the inventory catalog at add-time, never taken from the caller. "
                    + "Requires the MECHANIC or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No Budget exists with the given id, or the referenced part/service does not exist.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The Budget is locked (not in DRAFT) and cannot be edited.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/lines")
    ResponseEntity<BudgetInfo> addLine(
            @Parameter(description = "Budget id") @PathVariable UUID id,
            @Valid @RequestBody AddBudgetLineCommand command
    );

    @Operation(
            summary = "Remove a Budget line",
            description = "Removes a line from a Budget still in DRAFT. Requires the MECHANIC or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No Budget or Budget line exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The Budget is locked (not in DRAFT) and cannot be edited.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}/lines/{lineId}")
    ResponseEntity<BudgetInfo> removeLine(
            @Parameter(description = "Budget id") @PathVariable UUID id,
            @Parameter(description = "Budget line id") @PathVariable UUID lineId
    );

    @Operation(
            summary = "Change a Budget line's quantity",
            description = "Updates the quantity of an existing line on a Budget still in DRAFT, recalculating its total. "
                    + "Requires the MECHANIC or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No Budget or Budget line exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The Budget is locked (not in DRAFT) and cannot be edited.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}/lines/{lineId}/quantity")
    ResponseEntity<BudgetInfo> changeLineQuantity(
            @Parameter(description = "Budget id") @PathVariable UUID id,
            @Parameter(description = "Budget line id") @PathVariable UUID lineId,
            @Valid @RequestBody ChangeBudgetLineQuantityCommand command
    );

    @Operation(
            summary = "Send a Budget to the customer",
            description = "Sends the DRAFT Budget to the customer for approval, locking it against further edits. "
                    + "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No Budget exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The Budget is not in a state that allows sending (e.g. already sent).",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/send")
    ResponseEntity<BudgetInfo> send(@Parameter(description = "Budget id") @PathVariable UUID id);

    @Operation(
            summary = "Resend a Budget to the customer",
            description = "Re-sends a Budget to the customer, e.g. after it was refused and revised. "
                    + "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No Budget exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The Budget is not in a state that allows resending.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/resend")
    ResponseEntity<BudgetInfo> resend(@Parameter(description = "Budget id") @PathVariable UUID id);

    @Operation(
            summary = "View a Budget by decision token",
            description = "Public — no authentication required. Resolves the Budget the emailed decision link "
                    + "points at; possession of the token is the credential (ADR 0021). Keeps working after the "
                    + "Budget has been approved or refused, so a customer reopening the link sees the outcome."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "The token is invalid, or no Budget exists for it.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/decision/view")
    ResponseEntity<BudgetInfo> viewByToken(@Valid @RequestBody BudgetTokenCommand command);

    @Operation(
            summary = "Approve a Budget by decision token",
            description = "Public — no authentication required. Same SENT -> APPROVED transition as the "
                    + "authenticated approve endpoint; the token proves authorization instead of a JWT (ADR 0021)."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "The token is invalid, or no Budget exists for it.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The Budget is not in a state that allows approval (e.g. already resolved).",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/decision/approval")
    ResponseEntity<BudgetInfo> approveByToken(@Valid @RequestBody BudgetTokenCommand command);

    @Operation(
            summary = "Refuse a Budget by decision token",
            description = "Public — no authentication required. Same SENT -> REFUSED transition as the "
                    + "authenticated refuse endpoint, with an optional reason; the token proves authorization "
                    + "instead of a JWT (ADR 0021)."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "The token is invalid, or no Budget exists for it.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The Budget is not in a state that allows refusal (e.g. already resolved).",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/decision/refusal")
    ResponseEntity<BudgetInfo> refuseByToken(@Valid @RequestBody BudgetTokenRefusalCommand command);
}
