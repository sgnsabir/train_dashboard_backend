package com.banenor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Derailment Risk Analysis.
 *
 * <p>
 * Encapsulates key sensor values used for detecting derailment risk.
 * It includes vibration measurements (left and right), the maximum vibration observed,
 * the absolute time delay difference between left and right sensors, a risk flag,
 * and a descriptive anomaly message.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DerailmentRiskDTO {

    /**
     * The train number associated with the sensor data.
     */
    private Integer trainNo;

    /**
     * The timestamp when the sensor measurement was recorded.
     */
    private LocalDateTime measurementTime;

    /**
     * Measured vertical vibration on the left side.
     */
    private Double vibrationLeft;

    /**
     * Measured vertical vibration on the right side.
     */
    private Double vibrationRight;

    /**
     * The maximum vibration value between left and right.
     */
    private Double maxVibration;

    /**
     * The absolute difference between left and right time delay measurements.
     */
    private Double timeDelayDifference;

    /**
     * Flag indicating if a derailment risk is detected.
     */
    private Boolean riskDetected;

    /**
     * A descriptive message detailing which threshold(s) have been exceeded.
     */
    private String anomalyMessage;
}
