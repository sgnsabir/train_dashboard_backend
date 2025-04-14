package com.banenor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for representing the overall reliability health score of a train.
 *
 * This DTO includes:
 * - trainNo: the train number.
 * - healthScore: a percentage (e.g., 85.0 means 85% health).
 * - message: a descriptive message (e.g., "Excellent health", "Poor health, immediate maintenance required").
 * - measurementTime: the timestamp when the sensor metrics were recorded.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReliabilityHealthDTO {
    private Integer trainNo;
    private Double healthScore;
    private String message;
    private LocalDateTime measurementTime;
}
