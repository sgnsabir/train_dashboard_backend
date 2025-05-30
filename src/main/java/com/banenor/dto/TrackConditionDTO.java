package com.banenor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     * Label for the measurement point, e.g., "TP1", "TP2", etc.
     * This could be used for identifying specific measurement locations along the track.
     */
    private String measurementPoint;

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

    /**
     * GPS latitude of the vehicle.
     */
    private Double latitude;

    /**
     * GPS longitude of the vehicle.
     */
    private Double longitude;

    /**
     * Speed of the vehicle in kilometers per hour.
     */
    private Double speedKph;

    /**
     * Direction of travel in degrees (0° to 360°).
     */
    private Double directionDeg;
}
