package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Segment Analysis.
 * Encapsulates aggregated anomaly counts for a specific track segment,
 * plus left/right deltas and pass/fail flags for each metric.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentAnalysisDTO {

    /** The identifier of the track segment. */
    private Integer segmentId;

    /** Total number of sensor records in the segment. */
    private long totalRecords;

    /** Number of records where max vertical vibration exceeds threshold. */
    private long highVibrationCount;

    /** Number of records where lateral force exceeds threshold. */
    private long highLateralForceCount;

    /** Number of records where vertical force exceeds threshold. */
    private long highVerticalForceCount;

    /** True if combined anomaly ratio (vib + lat + vert) / totalRecords ≥ hotspot threshold. */
    private boolean hotSpot;

    // ---- New fields for left/right deltas and pass/fail ----

    /**
     * Difference between left and right high-vibration counts:
     * abs(countLeft – countRight).
     */
    private long vibrationDelta;

    /** True if vibrationDelta is below configured imbalance threshold. */
    private boolean vibrationPass;

    /**
     * Difference between left and right high lateral-force counts:
     * abs(countLeft – countRight).
     */
    private long lateralForceDelta;

    /** True if lateralForceDelta is below configured imbalance threshold. */
    private boolean lateralForcePass;

    /**
     * Difference between left and right high vertical-force counts:
     * abs(countLeft – countRight).
     */
    private long verticalForceDelta;

    /** True if verticalForceDelta is below configured imbalance threshold. */
    private boolean verticalForcePass;
}
