package com.banenor.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Event representing a maintenance risk.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRiskEvent {
    private Integer analysisId;
    private double riskScore;
    private LocalDateTime timestamp;
}
