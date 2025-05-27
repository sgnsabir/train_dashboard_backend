package com.banenor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PerformanceDTO {
    @NotNull(message = "Timestamp cannot be null")
    private LocalDateTime createdAt;
    @NotNull(message = "Speed cannot be null")
    private Double speed;
    private Double acceleration;
    private Double aoa;
    @NotNull(message = "Measurement point cannot be null")
    private String measurementPoint;
}
