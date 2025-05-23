package com.banenor.dto;

import java.time.LocalDateTime;
import lombok.Data;


@Data
public class SensorMetricsDTO {
    private Integer analysisId;
    private Double averageSpeed;
    private Double minSpeed;
    private Double maxSpeed;
    private Double speedVariance;
    private Double averageAcceleration;
    private Double minAcceleration;
    private Double maxAcceleration;
    private Double accelerationVariance;
    private Double averageAoa;
    private Double minAoa;
    private Double maxAoa;
    private Double aoaVariance;
    private Double averageVibrationLeft;
    private Double minVibrationLeft;
    private Double maxVibrationLeft;
    private Double vibrationLeftVariance;
    private Double averageVibrationRight;
    private Double minVibrationRight;
    private Double maxVibrationRight;
    private Double vibrationRightVariance;
    private Double averageVerticalForceLeft;
    private Double minVerticalForceLeft;
    private Double maxVerticalForceLeft;
    private Double verticalForceLeftVariance;
    private Double averageVerticalForceRight;
    private Double minVerticalForceRight;
    private Double maxVerticalForceRight;
    private Double verticalForceRightVariance;
    private Double averageLateralForceLeft;
    private Double minLateralForceLeft;
    private Double maxLateralForceLeft;
    private Double lateralForceLeftVariance;
    private Double averageLateralForceRight;
    private Double minLateralForceRight;
    private Double maxLateralForceRight;
    private Double lateralForceRightVariance;
    private Double averageLateralVibrationLeft;
    private Double minLateralVibrationLeft;
    private Double maxLateralVibrationLeft;
    private Double lateralVibrationLeftVariance;
    private Double averageLateralVibrationRight;
    private Double minLateralVibrationRight;
    private Double maxLateralVibrationRight;
    private Double lateralVibrationRightVariance;
    private Double averageLngL;
    private Double averageLngR;
    private Double minLngL;
    private Double maxLngL;
    private Double lngLVariance;
    private Double minLngR;
    private Double maxLngR;
    private Double lngRVariance;
    private Double averageAxleLoadLeft;
    private Double minAxleLoadLeft;
    private Double maxAxleLoadLeft;
    private Double axleLoadLeftVariance;
    private Double averageAxleLoadRight;
    private Double minAxleLoadRight;
    private Double maxAxleLoadRight;
    private Double axleLoadRightVariance;
    private Double averageEmissions;
    private Double minEmissions;
    private Double maxEmissions;
    private Double emissionsVariance;
    private Double riskScore;
    private String predictionMessage;

    /**
     * New field added to capture the sensor measurement timestamp.
     * This field is used to resolve getCreatedAt() calls.
     */
    private LocalDateTime createdAt;

    // ============================================
    // Aggregated Helper Methods
    // ============================================

    /**
     * Computes the overall average vibration using both left and right metrics.
     */
    public Double getAverageVibration() {
        if (averageVibrationLeft != null && averageVibrationRight != null) {
            return (averageVibrationLeft + averageVibrationRight) / 2.0;
        } else if (averageVibrationLeft != null) {
            return averageVibrationLeft;
        } else {
            return averageVibrationRight;
        }
    }

    /**
     * Computes the overall average vertical force by averaging left and right values.
     */
    public Double getAverageVerticalForce() {
        if (averageVerticalForceLeft != null && averageVerticalForceRight != null) {
            return (averageVerticalForceLeft + averageVerticalForceRight) / 2.0;
        } else if (averageVerticalForceLeft != null) {
            return averageVerticalForceLeft;
        } else {
            return averageVerticalForceRight;
        }
    }

    /**
     * Computes the overall average lateral force by averaging left and right values.
     */
    public Double getAverageLateralForce() {
        if (averageLateralForceLeft != null && averageLateralForceRight != null) {
            return (averageLateralForceLeft + averageLateralForceRight) / 2.0;
        } else if (averageLateralForceLeft != null) {
            return averageLateralForceLeft;
        } else {
            return averageLateralForceRight;
        }
    }

    /**
     * Computes the overall average lateral vibration by averaging left and right values.
     */
    public Double getAverageLateralVibration() {
        if (averageLateralVibrationLeft != null && averageLateralVibrationRight != null) {
            return (averageLateralVibrationLeft + averageLateralVibrationRight) / 2.0;
        } else if (averageLateralVibrationLeft != null) {
            return averageLateralVibrationLeft;
        } else {
            return averageLateralVibrationRight;
        }
    }
}
