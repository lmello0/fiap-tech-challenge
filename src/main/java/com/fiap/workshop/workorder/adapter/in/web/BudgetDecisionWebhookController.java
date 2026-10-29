package com.fiap.workshop.workorder.adapter.in.web;

import com.fiap.workshop.workorder.application.port.in.DecideBudgetExternallyUseCase;
import com.fiap.workshop.workorder.application.port.in.DecideBudgetExternallyUseCase.Decision;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
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

	record Payload(@NotNull UUID eventId, @NotBlank String workOrderNumber, @NotNull UUID budgetId, @NotNull Decision decision,
			@Size(max = 500) String reason) {
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

	@PostMapping(value = "/budget-decisions", consumes = "application/json")
	Map<String, String> receive(@RequestHeader(value = "X-Timestamp", required = false) String timestamp,
			@RequestHeader(value = "X-Signature", required = false) String signature, @RequestBody String body) {
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
		return Map.of("result", result.name());
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
