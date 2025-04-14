package com.banenor.dto;

import lombok.Data;
import java.util.List;

/**
 * Data Transfer Object for Admin Dashboard.
 * Contains aggregated sensor metrics, alert history, and system status.
 */
@Data
public class AdminDashboardDTO {
    private Double averageSpeed;
    private Double averageAoa;
    private Double averageVibration;
    private Double averageVerticalForceLeft;
    private Double averageVerticalForceRight;
    private Double averageLateralForceLeft;
    private Double averageLateralForceRight;
    private Double averageLateralVibrationLeft;
    private Double averageLateralVibrationRight;
    private List<AlertResponse> alertHistory;
    private String systemStatus;
}
