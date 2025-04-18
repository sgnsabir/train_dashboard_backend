package com.banenor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Performance Domain Metrics.
 * This DTO aggregates key performance parameters (e.g., speed, acceleration)
 * computed from the sensor data stored in the existing model (e.g., HaugfjellMP1Axles, HaugfjellMP3Axles).
 */
@Data
public class PerformanceDTO {

    /**
     * Timestamp corresponding to the sensor reading.
     */
    @NotNull(message = "Timestamp cannot be null")
    private LocalDateTime createdAt;

    /**
     * Measured or computed speed, in km/h.
     */
    @NotNull(message = "Speed cannot be null")
    private Double speed;

    /**
     * Computed acceleration (if available), in m/s².
     * This may be calculated from consecutive speed readings.
     */
    private Double acceleration;


    @NotNull(message = "Measurement point cannot be null")
    private String measurementPoint;
}
