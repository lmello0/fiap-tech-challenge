package com.fiap.techchallenge.inventory.controllers;

import com.fiap.techchallenge.inventory.api.commands.CreateReorderRuleCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateReorderRuleCommand;
import com.fiap.techchallenge.inventory.api.queries.ReorderRuleFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.ReorderRuleInfo;
import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import com.fiap.techchallenge.shared.responses.PageResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

/** Full Swagger/OpenAPI contract for {@link ReorderRuleController} — per-part, per-vendor
 * automatic-reorder thresholds. */
@Tag(name = "Reorder Rules", description = "Per-part, per-vendor automatic-reorder thresholds.")
@RequestMapping("reorder-rules")
public interface ReorderRuleControllerSwaggerDoc {

    @Operation(
            summary = "List reorder rules",
            description = "Returns a paginated, filterable list of reorder rules. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @GetMapping
    ResponseEntity<PageResponse<ReorderRuleInfo>> getAll(
            @PageableDefault Pageable pageable,
            ReorderRuleFilterQuery filter
    );

    @Operation(
            summary = "Get a reorder rule by id",
            description = "Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No reorder rule exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<ReorderRuleInfo> getById(@Parameter(description = "Reorder rule id") @PathVariable UUID id);

    @Operation(
            summary = "Create a reorder rule",
            description = "Creates a minimum/maximum quantity threshold for a part, pointing at the vendor to reorder "
                    + "from. Evaluated immediately, so a part already below the given minimum triggers a reorder "
                    + "signal right away. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "The referenced part or vendor does not exist.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<ReorderRuleInfo> create(@Valid @RequestBody CreateReorderRuleCommand command);

    @Operation(
            summary = "Update a reorder rule",
            description = "Updates a reorder rule's thresholds, vendor, and enabled flag, and re-evaluates the part "
                    + "immediately. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No reorder rule exists with the given id, or the referenced vendor does not exist.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}")
    ResponseEntity<ReorderRuleInfo> update(
            @Parameter(description = "Reorder rule id") @PathVariable UUID id,
            @Valid @RequestBody UpdateReorderRuleCommand command
    );

    @Operation(
            summary = "Delete a reorder rule",
            description = "Permanently deletes a reorder rule. Requires the STOCKIST or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "204", description = "The reorder rule was deleted.")
    @ApiResponse(responseCode = "404", description = "No reorder rule exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@Parameter(description = "Reorder rule id") @PathVariable UUID id);
}
