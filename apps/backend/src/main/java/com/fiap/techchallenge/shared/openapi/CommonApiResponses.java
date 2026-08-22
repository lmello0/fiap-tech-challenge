package com.fiap.techchallenge.shared.openapi;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ProblemDetail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The three error outcomes that apply to virtually every authenticated endpoint in this API, so
 * each endpoint's own doc only needs to declare the outcomes specific to it (404, 409, ...) on top
 * of this. All error bodies are RFC 7807 {@link ProblemDetail}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "The request body or parameters failed validation.",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
                responseCode = "401",
                description = "No valid JWT bearer token was supplied.",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
                responseCode = "403",
                description = "The caller is authenticated but does not hold a required role.",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
})
public @interface CommonApiResponses {
}
