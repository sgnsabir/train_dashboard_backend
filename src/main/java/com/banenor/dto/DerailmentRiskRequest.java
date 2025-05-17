package com.banenor.dto;

import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

@Data
@ParameterObject
public class DerailmentRiskRequest {

    /**
     * Train number for which to fetch derailment risk.
     */
    @Min(value = 1, message = "trainNo must be at least 1")
    private Integer trainNo;

    /**
     * Optional ISO-8601 start timestamp. Defaults applied in controller.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime start;

    /**
     * Optional ISO-8601 end timestamp. Defaults applied in controller.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime end;
}
