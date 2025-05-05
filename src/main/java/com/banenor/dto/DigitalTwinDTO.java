package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing the state of a digital twin for a train asset.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalTwinDTO {
    /**
     * Identifier for the digital twin asset (typically the train number).
     */
    private Integer assetId;

    /**
     * Current status of the asset (e.g., "Operational", "Maintenance Required").
     */
    private String status;

    /**
     * Last updated timestamp of the digital twin.
     */
    private LocalDateTime updatedAt;

    /**
     * A summary string of key sensor metrics.
     */
    private String sensorSummary;
}
