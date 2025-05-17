package com.banenor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@ParameterObject
@Schema(description = "Parameters for fetching track condition data")
public class TrackConditionRequest {

    @Schema(description = "Train number (must be ≥ 1)", required = true, example = "42")
    @Min(value = 1, message = "trainNo must be ≥ 1")
    private Integer trainNo;

    @Schema(description = "Window start timestamp (ISO-8601)", example = "2025-05-10T00:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime start;

    @Schema(description = "Window end timestamp (ISO-8601)", example = "2025-05-17T00:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime end;
}
