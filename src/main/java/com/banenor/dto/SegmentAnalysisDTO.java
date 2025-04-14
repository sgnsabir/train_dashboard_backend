package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Segment Analysis.
 * Encapsulates aggregated anomaly counts for a specific track segment.
 * Fields include:
 * segmentId: The identifier of the track segment.
 * totalRecords: Total number of sensor records in the segment.
 * highVibrationCount: Number of records where the maximum vertical vibration (left/right)
 *       exceeds the configured vibration threshold.
 * highLateralForceCount: Number of records where the lateral force (left/right) exceeds
 *       the configured high lateral force threshold.
 * highVerticalForceCount: Number of records where the vertical force (left/right) exceeds
 *       the configured high vertical force threshold.
 * hotSpot: A flag set to true if the combined anomaly ratio exceeds the defined threshold.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentAnalysisDTO {
    private Integer segmentId;
    private long totalRecords;
    private long highVibrationCount;
    private long highLateralForceCount;
    private long highVerticalForceCount;
    private boolean hotSpot;
}
