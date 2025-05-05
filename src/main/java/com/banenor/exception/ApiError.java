package com.banenor.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Standard API error response")
public class ApiError {

    @Schema(description = "Machine-readable error code", example = "ERR-400")
    private String code;

    @Schema(description = "Human-readable message", example = "Validation failed")
    private String message;

    @Schema(description = "Optional list of specific validation or error details")
    private List<String> details;

    @Schema(description = "Timestamp when the error occurred")
    private LocalDateTime timestamp;

    @Schema(description = "Correlation ID for tracing", example = "123e4567-e89b-12d3-a456-426614174000")
    private String correlationId;
}
