package com.banenor.dto;

import lombok.Data;

@Data
public class PredictiveMaintenanceDTO {
    private Integer analysisId;
    private Double averageSpeed;
    private Double speedVariance;
    private Double averageAoa;
    private Double averageVibration;
    private Double averageVerticalForce;
    private Double averageLateralForce;
    private Double averageLateralVibration;
    private Double riskScore;
    private String predictionMessage;
}
