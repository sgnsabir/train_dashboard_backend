package com.banenor.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springdoc.api.annotations.ParameterObject;

import java.time.LocalDateTime;

/**
 * Wrapper DTO for wheel condition query parameters.
 */
@Data
@ParameterObject
public class WheelConditionRequest {

    @Min(value = 1, message = "trainNo must be at least 1")
    private Integer trainNo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime start;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime end;
}
