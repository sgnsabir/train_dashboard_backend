package com.banenor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Track Condition Analysis.
 * This DTO encapsulates sensor measurements related to track forces.
 * It includes the train number, measurement time, lateral forces (left and right),
 * vertical forces (left and right), and flags indicating if high lateral or vertical forces were detected.
 * Additionally, it provides an anomaly message to detail any threshold breaches.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackConditionDTO {

    /**
     * Train number associated with the sensor record.
     */
    private Integer trainNo;

    /**
     * Timestamp when the sensor measurement was recorded.
     */
    private LocalDateTime measurementTime;

    /**
     * Measured lateral force on the left side.
     */
    private Double lateralForceLeft;

    /**
     * Measured lateral force on the right side.
     */
    private Double lateralForceRight;

    /**
     * Measured vertical force on the left side.
     */
    private Double verticalForceLeft;

    /**
     * Measured vertical force on the right side.
     */
    private Double verticalForceRight;

    /**
     * Flag indicating if high lateral force is detected.
     */
    private Boolean highLateralForce;

    /**
     * Flag indicating if high vertical force is detected.
     */
    private Boolean highVerticalForce;

    /**
     * Descriptive message detailing any detected anomalies.
     */
    private String anomalyMessage;
}
