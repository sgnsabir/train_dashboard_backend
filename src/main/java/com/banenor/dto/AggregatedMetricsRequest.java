package com.banenor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

/**
 * Request DTO for fetching aggregated sensor metrics.
 * <p>
 * Both startDate and endDate are required and must be in ISO-8601 format,
 * e.g. {@code 2025-05-01T00:00:00}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Validated
@ParameterObject
public class AggregatedMetricsRequest {

    /**
     * ISO-8601 start date for aggregation.
     */
    @NotNull(message = "startDate is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    /**
     * ISO-8601 end date for aggregation.
     */
    @NotNull(message = "endDate is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;
}
