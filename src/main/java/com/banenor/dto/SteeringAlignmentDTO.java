package com.banenor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Steering Alignment Analysis.
 *
 * This DTO encapsulates sensor measurements used to assess steering misalignment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SteeringAlignmentDTO {
    private Integer trainNo;
    private LocalDateTime measurementTime;
    // Absolute angle of attack (derived from aoa_tp1)
    private Double angleOfAttack;
    // Lateral forces from left and right (from lfrcl_tp1 and lfrcr_tp1)
    private Double lateralForceLeft;
    private Double lateralForceRight;
    // Vertical forces from left and right (from vfrcl_tp1 and vfrcr_tp1)
    private Double verticalForceLeft;
    private Double verticalForceRight;
    // Computed lateral-to-vertical force ratio
    private Double lateralVerticalRatio;
    // Flag indicating if the angle of attack exceeds the threshold
    private Boolean aoaOutOfSpec;
    // Flag indicating if the lateral-to-vertical ratio exceeds the threshold
    private Boolean misalignmentDetected;
    // Descriptive message summarizing anomalies
    private String anomalyMessage;
}
