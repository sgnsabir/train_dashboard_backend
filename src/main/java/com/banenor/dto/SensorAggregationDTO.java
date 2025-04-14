package com.banenor.dto;

import lombok.Data;

@Data
public class SensorAggregationDTO {
    // Speed metrics
    private Double averageSpeed;
    private Double minSpeed;
    private Double maxSpeed;
    private Double speedVariance;

    // Angle of Attack metrics
    private Double averageAoa;
    private Double minAoa;
    private Double maxAoa;
    private Double aoaVariance;

    // Vibration metrics
    private Double averageVibration;
    private Double minVibration;
    private Double maxVibration;
    private Double vibrationVariance;

    // Vertical Force Left (Axle Load)
    private Double averageVerticalForceLeft;
    private Double minVerticalForceLeft;
    private Double maxVerticalForceLeft;
    private Double verticalForceLeftVariance;

    // Lateral Force Left
    private Double averageLateralForceLeft;
    private Double minLateralForceLeft;
    private Double maxLateralForceLeft;
    private Double lateralForceLeftVariance;

    // Lateral Vibration Left
    private Double averageLateralVibrationLeft;
    private Double minLateralVibrationLeft;
    private Double maxLateralVibrationLeft;
    private Double lateralVibrationLeftVariance;
}
