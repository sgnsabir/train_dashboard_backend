package com.banenor.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springdoc.api.annotations.ParameterObject;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * Wrapper for segment analysis request parameters.
 */
@Data
@ParameterObject
public class SegmentAnalysisRequest {

    @Min(value = 1, message = "trainNo must be greater than zero")
    private Integer trainNo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime start;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime end;
}
