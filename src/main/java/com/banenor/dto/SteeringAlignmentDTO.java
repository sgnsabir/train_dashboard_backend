package com.banenor.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Steering Alignment Analysis.
 * Encapsulates per-TP sensor measurements plus computed deltas
 * and overall pass/fail flag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SteeringAlignmentDTO {

    /** Train number for which this measurement applies. */
    private Integer trainNo;

    /** Timestamp of the measurement. */
    private LocalDateTime measurementTime;

    /** Absolute angle of attack at this TP. */
    private Double angleOfAttack;

    /** Lateral force (left) at this TP. */
    private Double lateralForceLeft;

    /** Lateral force (right) at this TP. */
    private Double lateralForceRight;

    /** Vertical force (left) at this TP. */
    private Double verticalForceLeft;

    /** Vertical force (right) at this TP. */
    private Double verticalForceRight;

    /** Computed lateral/vertical force ratio: (latL+latR)/(vertL+vertR). */
    private Double lateralVerticalRatio;

    /** True if AOA > configured threshold. */
    private Boolean aoaOutOfSpec;

    /** True if lateralVerticalRatio > configured threshold. */
    private Boolean misalignmentDetected;

    /** Descriptive summary of any anomalies at this TP. */
    private String anomalyMessage;

    // ---- New delta / pass-fail fields ----

    /**
     * Absolute difference between left and right lateral forces.
     */
    private Double lateralForceDelta;

    /** True if lateralForceDelta ≤ configured lateral‐imbalance threshold. */
    private Boolean lateralBalancePass;

    /**
     * Absolute difference between left and right vertical forces.
     */
    private Double verticalForceDelta;

    /** True if verticalForceDelta ≤ configured vertical‐imbalance threshold. */
    private Boolean verticalBalancePass;

    /**
     * Overall pass if none of the individual checks (AOA, lat imbalance, vert imbalance)
     * are failing.
     */
    private Boolean overallAlignmentPass;
}
