package com.banenor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ParameterObject
@Schema(description = "Parameters for fetching per‐train axles data")
public class AxlesDataRequest {

    @NotNull(message = "trainNo is required")
    @Schema(description = "Train number", example = "42", required = true)
    private Integer trainNo;

    @NotNull(message = "start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Window start (ISO date-time)", example = "2025-05-01T00:00:00", required = true)
    private LocalDateTime start;

    @NotNull(message = "end time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Window end (ISO date-time)", example = "2025-05-02T00:00:00", required = true)
    private LocalDateTime end;

    @Schema(description = "Optional measurement point filter (e.g. TP1)", example = "TP1")
    private String measurementPoint;
}
