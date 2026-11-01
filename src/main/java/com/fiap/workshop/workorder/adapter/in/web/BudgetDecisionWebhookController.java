package com.fiap.workshop.workorder.adapter.in.web;

import com.fiap.workshop.workorder.application.port.in.DecideBudgetExternallyUseCase;
import com.fiap.workshop.workorder.application.port.in.DecideBudgetExternallyUseCase.Decision;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/v1/webhooks")
class BudgetDecisionWebhookController {

	@Schema(name = "BudgetDecisionWebhookPayload")
	record Payload(@NotNull UUID eventId, @NotBlank String workOrderNumber, @NotNull UUID budgetId, @NotNull Decision decision,
			@Size(max = 500) String reason) {
	}

	@Schema(name = "BudgetDecisionWebhookReceipt")
	record Receipt(DecideBudgetExternallyUseCase.Result result) {
	}

	static class InvalidSignatureException extends RuntimeException {
	}

	private final WebhookSignatureVerifier signatures;
	private final DecideBudgetExternallyUseCase decide;
	private final JsonMapper json;
	private final Validator validator;

	BudgetDecisionWebhookController(WebhookSignatureVerifier signatures, DecideBudgetExternallyUseCase decide, JsonMapper json,
			Validator validator) {
		this.signatures = signatures;
		this.decide = decide;
		this.json = json;
		this.validator = validator;
	}

	// The body is read as a String so the signature is checked against the exact bytes the partner signed.
	@Operation(summary = "Receive a budget decision from the partner system",
			description = """
					No bearer token: the request is authenticated by an HMAC signature of the raw body. \
					X-Signature must be `sha256=` followed by the hex HMAC-SHA256 of `<X-Timestamp>.<body>`, keyed by the shared \
					webhook secret, and X-Timestamp must be within the configured tolerance (5 minutes by default). \
					Delivering the same eventId again is acknowledged as DUPLICATE without applying the decision twice.""",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = Payload.class))))
	@SecurityRequirements
	@PostMapping(value = "/budget-decisions", consumes = "application/json")
	Receipt receive(
			@Parameter(description = "Unix time, in seconds, when the request was signed", example = "1761998400")
			@RequestHeader(value = "X-Timestamp", required = false) String timestamp,
			@Parameter(description = "`sha256=` + hex HMAC-SHA256 of `<X-Timestamp>.<body>`")
			@RequestHeader(value = "X-Signature", required = false) String signature,
			@RequestBody String body) {
		if (!signatures.isValid(timestamp, body, signature)) {
			throw new InvalidSignatureException();
		}
		Payload payload = json.readValue(body, Payload.class);
		var violations = validator.validate(payload);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
		var result = decide.execute(new DecideBudgetExternallyUseCase.Command(payload.eventId(), payload.workOrderNumber(),
				payload.budgetId(), payload.decision(), payload.reason()));
		return new Receipt(result);
	}

	@ExceptionHandler(InvalidSignatureException.class)
	ProblemDetail invalidSignature() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid or expired webhook signature");
	}

	@ExceptionHandler({ConstraintViolationException.class, tools.jackson.core.JacksonException.class})
	ProblemDetail invalidPayload(Exception e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid webhook payload");
	}
}
