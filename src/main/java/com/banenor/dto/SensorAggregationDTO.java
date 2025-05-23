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

    // Vibration metrics (left/right)
    private Double averageVibrationLeft;
    private Double minVibrationLeft;
    private Double maxVibrationLeft;
    private Double vibrationLeftVariance;

    private Double averageVibrationRight;
    private Double minVibrationRight;
    private Double maxVibrationRight;
    private Double vibrationRightVariance;

    // Vertical Force metrics (left/right)
    private Double averageVerticalForceLeft;
    private Double minVerticalForceLeft;
    private Double maxVerticalForceLeft;
    private Double verticalForceLeftVariance;

    private Double averageVerticalForceRight;
    private Double minVerticalForceRight;
    private Double maxVerticalForceRight;
    private Double verticalForceRightVariance;

    // Lateral Force metrics (left/right)
    private Double averageLateralForceLeft;
    private Double minLateralForceLeft;
    private Double maxLateralForceLeft;
    private Double lateralForceLeftVariance;

    private Double averageLateralForceRight;
    private Double minLateralForceRight;
    private Double maxLateralForceRight;
    private Double lateralForceRightVariance;

    // Lateral Vibration metrics (left/right)
    private Double averageLateralVibrationLeft;
    private Double minLateralVibrationLeft;
    private Double maxLateralVibrationLeft;
    private Double lateralVibrationLeftVariance;

    private Double averageLateralVibrationRight;
    private Double minLateralVibrationRight;
    private Double maxLateralVibrationRight;
    private Double lateralVibrationRightVariance;

    private  String vit;
    private  Double avgSpeed;
}
