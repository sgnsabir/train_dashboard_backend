// src/main/java/com/banenor/dto/DigitalTwinInsightDTO.java
package com.banenor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalTwinDTO {
    /** The timestamp of the data point */
    private LocalDateTime timestamp;

    /** The value of the metric (e.g., temperature, load, etc.) */
    private Double value;

    /** The type of insight (e.g., "wheelCondition", "bogieLoad", etc.) */
    private String type;

    /** The component to which the insight relates (e.g., "wheel", "bogie") */
    private String component;

    /** Optional field for location if available */
    private String location;

    /** Optional asset identifier if part of system context */
    private String assetId;

    /** Optional field for the status of the component or asset (e.g., "healthy", "faulty") */
    private String status;

    /** Optional risk level if applicable for the insight */
    private RiskLevel riskLevel;

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }
}
